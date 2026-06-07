#pragma once
/**
 * Google Login Proxy Integration for Virtual Blackbox SDK
 * BGMI / PUBG Mobile
 */

#include <string>
#include <thread>
#include <chrono>
#include <functional>
#include <cctype>
#include <cstdio>
#include <cstdlib>

#include <json/json.h>

namespace SDK {
namespace Auth {

constexpr const char *PROXY_SERVER_HOST = "127.0.0.1";
constexpr int PROXY_SERVER_PORT = 8080;
constexpr int POLL_INTERVAL_MS = 2000;
constexpr int MAX_POLL_ATTEMPTS = 60;

struct GoogleAuthToken {
    std::string access_token;
    std::string refresh_token;
    std::string token_type;
    int expires_in = 0;
    std::string scope;
    long long created_at = 0;

    bool IsExpired() const {
        if (created_at == 0 || expires_in == 0) return true;
        auto now = std::chrono::system_clock::now().time_since_epoch().count() / 10000000;
        return (now - created_at) >= (expires_in - 300);
    }

    bool IsValid() const {
        return !access_token.empty() && !IsExpired();
    }
};

enum class ELoginStatus {
    Idle,
    Pending,
    Success,
    Failed,
    Cancelled,
};

using LoginSuccessCallback = std::function<void(const GoogleAuthToken &)>;
using LoginFailedCallback = std::function<void(const std::string &error)>;
using StatusUpdateCallback = std::function<void(ELoginStatus status, const std::string &message)>;

class GoogleLoginProxy {
public:
    static GoogleLoginProxy &Get() {
        static GoogleLoginProxy instance;
        return instance;
    }

    bool Login(const std::string &device_id, LoginSuccessCallback on_success,
               LoginFailedCallback on_failed, StatusUpdateCallback on_status = nullptr) {
        if (m_is_running) {
            if (on_failed) on_failed("Login already in progress");
            return false;
        }

        m_is_running = true;
        m_status = ELoginStatus::Pending;
        m_callbacks = {on_success, on_failed, on_status};

        if (!RequestAuthSession(device_id)) {
            m_is_running = false;
            m_status = ELoginStatus::Failed;
            if (on_failed) on_failed("Failed to create auth session");
            return false;
        }

        OpenBrowserForAuth();
        m_poll_thread = std::thread(&GoogleLoginProxy::PollForToken, this);
        m_poll_thread.detach();
        return true;
    }

    bool LoginBlocking(const std::string &device_id, int timeout_ms, GoogleAuthToken &token) {
        bool completed = false;
        bool success = false;

        Login(device_id,
              [&](const GoogleAuthToken &t) {
                  token = t;
                  success = true;
                  completed = true;
              },
              [&](const std::string &) { completed = true; },
              [&](ELoginStatus, const std::string &) {});

        int waited = 0;
        while (!completed && (timeout_ms == 0 || waited < timeout_ms)) {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            waited += 100;
        }
        return success;
    }

    void Cancel() {
        m_is_running = false;
        m_status = ELoginStatus::Cancelled;
    }

    ELoginStatus GetStatus() const { return m_status; }
    bool IsRunning() const { return m_is_running; }
    const GoogleAuthToken &GetLastToken() const { return m_last_token; }

    bool RefreshToken(GoogleAuthToken &token) {
        if (token.refresh_token.empty()) return false;
        return HttpPostRefreshToken(token.refresh_token, token);
    }

private:
    GoogleLoginProxy() = default;
    ~GoogleLoginProxy() = default;
    GoogleLoginProxy(const GoogleLoginProxy &) = delete;
    GoogleLoginProxy &operator=(const GoogleLoginProxy &) = delete;

    struct Callbacks {
        LoginSuccessCallback on_success;
        LoginFailedCallback on_failed;
        StatusUpdateCallback on_status;
    };

    bool m_is_running = false;
    ELoginStatus m_status = ELoginStatus::Idle;
    std::string m_session_id;
    std::string m_auth_url;
    GoogleAuthToken m_last_token;
    Callbacks m_callbacks;
    std::thread m_poll_thread;

    bool RequestAuthSession(const std::string &device_id) {
        std::string url = GetProxyUrl("/sdk/auth");
        std::string post_data = "device_id=" + UrlEncode(device_id) + "&package=com.pubg.imobile";
        std::string response;
        if (!HttpPost(url, post_data, response)) return false;

        Json::Value root;
        Json::Reader reader;
        if (!reader.parse(response, root)) return false;
        if (root.isMember("error")) return false;

        m_session_id = root["session_id"].asString();
        m_auth_url = root["auth_url"].asString();
        return !m_session_id.empty() && !m_auth_url.empty();
    }

    void OpenBrowserForAuth() {
#ifdef _WIN32
        std::string cmd = "start \"\" \"" + m_auth_url + "\"";
        system(cmd.c_str());
#elif __APPLE__
        std::string cmd = "open \"" + m_auth_url + "\"";
        system(cmd.c_str());
#elif __ANDROID__
        ShowAndroidToast("Open browser and login: " + m_auth_url);
#else
        std::string cmd = "xdg-open \"" + m_auth_url + "\"";
        system(cmd.c_str());
#endif
        UpdateStatus(ELoginStatus::Pending, "Browser opened. Please complete Google login.");
    }

    void PollForToken() {
        int attempts = 0;
        while (m_is_running && attempts < MAX_POLL_ATTEMPTS) {
            std::this_thread::sleep_for(std::chrono::milliseconds(POLL_INTERVAL_MS));
            if (!m_is_running) break;

            std::string status_response;
            if (!HttpGet(GetProxyUrl("/auth/status?session=" + m_session_id), status_response)) {
                attempts++;
                continue;
            }

            Json::Value status_json;
            Json::Reader reader;
            if (!reader.parse(status_response, status_json)) {
                attempts++;
                continue;
            }

            std::string status = status_json["status"].asString();
            if (status == "success") {
                std::string token_response;
                if (HttpGet(GetProxyUrl("/auth/token?session=" + m_session_id), token_response)) {
                    Json::Value token_json;
                    if (reader.parse(token_response, token_json)) {
                        ParseAndStoreToken(token_json["token"]);
                        m_is_running = false;
                        m_status = ELoginStatus::Success;
                        if (m_callbacks.on_success) m_callbacks.on_success(m_last_token);
                        UpdateStatus(ELoginStatus::Success, "Login successful!");
                        return;
                    }
                }
            } else if (status == "failed") {
                m_is_running = false;
                m_status = ELoginStatus::Failed;
                if (m_callbacks.on_failed) m_callbacks.on_failed("Login failed on server side");
                UpdateStatus(ELoginStatus::Failed, "Login failed");
                return;
            }

            attempts++;
            UpdateStatus(ELoginStatus::Pending, "Waiting for login... (" + std::to_string(attempts) + "/" +
                                                    std::to_string(MAX_POLL_ATTEMPTS) + ")");
        }

        if (m_is_running) {
            m_is_running = false;
            m_status = ELoginStatus::Failed;
            if (m_callbacks.on_failed) m_callbacks.on_failed("Login timeout - user did not complete login in time");
            UpdateStatus(ELoginStatus::Failed, "Login timeout");
        }
    }

    void ParseAndStoreToken(const Json::Value &token_json) {
        m_last_token.access_token = token_json["access_token"].asString();
        m_last_token.refresh_token = token_json.get("refresh_token", "").asString();
        m_last_token.token_type = token_json.get("token_type", "Bearer").asString();
        m_last_token.expires_in = token_json.get("expires_in", 3600).asInt();
        m_last_token.scope = token_json.get("scope", "").asString();
        m_last_token.created_at = std::chrono::system_clock::now().time_since_epoch().count() / 10000000;
    }

    bool HttpPostRefreshToken(const std::string &refresh_token, GoogleAuthToken &token) {
        std::string response;
        if (!HttpPost(GetProxyUrl("/sdk/refresh"), "refresh_token=" + UrlEncode(refresh_token), response)) return false;

        Json::Value root;
        Json::Reader reader;
        if (!reader.parse(response, root)) return false;
        if (root.isMember("error")) return false;

        ParseAndStoreToken(root["token"]);
        token = m_last_token;
        return true;
    }

    void UpdateStatus(ELoginStatus status, const std::string &message) {
        m_status = status;
        if (m_callbacks.on_status) m_callbacks.on_status(status, message);
    }

    std::string GetProxyUrl(const std::string &endpoint) {
        return "http://" + std::string(PROXY_SERVER_HOST) + ":" + std::to_string(PROXY_SERVER_PORT) + endpoint;
    }

    std::string UrlEncode(const std::string &value) {
        std::string encoded;
        for (char c : value) {
            if (std::isalnum(static_cast<unsigned char>(c)) || c == '-' || c == '_' || c == '.' || c == '~') {
                encoded += c;
            } else {
                char buf[4];
                std::snprintf(buf, sizeof(buf), "%%%02X", static_cast<unsigned char>(c));
                encoded += buf;
            }
        }
        return encoded;
    }

    bool HttpGet(const std::string &, std::string &) { return false; }
    bool HttpPost(const std::string &, const std::string &, std::string &) { return false; }
    void ShowAndroidToast(const std::string &) {}
};

} // namespace Auth
} // namespace SDK
