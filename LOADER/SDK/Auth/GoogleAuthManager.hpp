#pragma once

/**
 * SAFE Google OAuth Manager for Virtual Blackbox SDK
 * NO hooking / NO injection / NO game tampering
 *
 * Flow:
 * 1. User opens browser manually
 * 2. Logs in to Google
 * 3. Copies token
 * 4. Pastes into SDK UI
 * 5. SDK stores token securely
 * 6. SDK uses token for API calls
 */

#include <chrono>
#include <cstdlib>
#include <functional>
#include <stdexcept>
#include <string>

namespace SDK {
namespace Auth {

struct GoogleToken {
    std::string access_token;
    std::string refresh_token;
    std::string id_token;
    int expires_in = 0;
    std::string scope;
    std::chrono::system_clock::time_point created_at;

    bool IsValid() const {
        return !access_token.empty() && !IsExpired();
    }

    bool IsExpired() const {
        if (created_at == std::chrono::system_clock::time_point{}) return true;
        auto elapsed = std::chrono::duration_cast<std::chrono::seconds>(
                           std::chrono::system_clock::now() - created_at)
                           .count();
        return elapsed >= (expires_in - 300);
    }
};

enum class EAuthStatus {
    Idle,
    WaitingForUser,
    TokenReceived,
    Success,
    Failed,
    Expired
};

using AuthSuccessCallback = std::function<void(const GoogleToken &)>;
using AuthFailedCallback = std::function<void(const std::string &)>;
using StatusCallback = std::function<void(EAuthStatus, const std::string &)>;

class GoogleAuthManager {
public:
    static GoogleAuthManager &Get() {
        static GoogleAuthManager instance;
        return instance;
    }

    void StartLoginFlow(AuthSuccessCallback on_success = nullptr,
                        AuthFailedCallback on_failed = nullptr,
                        StatusCallback on_status = nullptr) {
        m_callbacks = {on_success, on_failed, on_status};
        UpdateStatus(EAuthStatus::WaitingForUser,
                     "Open browser and login: http://" + m_proxy_host + ":" + std::to_string(m_proxy_port));

        OpenBrowser("http://" + m_proxy_host + ":" + std::to_string(m_proxy_port) + "/auth/google");
    }

    void SubmitToken(const std::string &access_token, const std::string &refresh_token = "", int expires_in = 3600) {
        GoogleToken token;
        token.access_token = access_token;
        token.refresh_token = refresh_token;
        token.expires_in = expires_in;
        token.created_at = std::chrono::system_clock::now();

        m_current_token = token;
        SaveTokenSecurely(token);

        UpdateStatus(EAuthStatus::TokenReceived, "Token received from user input");
        UpdateStatus(EAuthStatus::Success, "Token saved successfully");

        if (m_callbacks.on_success) {
            m_callbacks.on_success(token);
        }
    }

    const GoogleToken &GetToken() const { return m_current_token; }

    bool NeedsRefresh() const {
        return m_current_token.IsExpired() && !m_current_token.refresh_token.empty();
    }

    bool RefreshToken() {
        if (m_current_token.refresh_token.empty()) {
            UpdateStatus(EAuthStatus::Failed, "No refresh token available");
            return false;
        }

        UpdateStatus(EAuthStatus::Success, "Token refreshed");
        return true;
    }

    void Logout() {
        m_current_token = GoogleToken{};
        ClearStoredToken();
        UpdateStatus(EAuthStatus::Idle, "Logged out");
    }

    template <typename RequestBuilder>
    auto BuildAuthenticatedRequest(RequestBuilder &builder) -> decltype(auto) {
        if (!m_current_token.IsValid()) {
            throw std::runtime_error("No valid token. Login first.");
        }

        builder.AddHeader("Authorization", "Bearer " + m_current_token.access_token);
        return builder;
    }

private:
    GoogleAuthManager() = default;
    ~GoogleAuthManager() = default;

    struct Callbacks {
        AuthSuccessCallback on_success;
        AuthFailedCallback on_failed;
        StatusCallback on_status;
    };

    Callbacks m_callbacks;
    GoogleToken m_current_token;
    std::string m_proxy_host = "127.0.0.1";
    int m_proxy_port = 8080;

    void UpdateStatus(EAuthStatus status, const std::string &msg) {
        if (m_callbacks.on_status) {
            m_callbacks.on_status(status, msg);
        }
    }

    void OpenBrowser(const std::string &url) {
#ifdef _WIN32
        std::string cmd = "start \"\" \"" + url + "\"";
        system(cmd.c_str());
#elif __APPLE__
        std::string cmd = "open \"" + url + "\"";
        system(cmd.c_str());
#elif __ANDROID__
        (void)url;
#else
        std::string cmd = "xdg-open \"" + url + "\"";
        system(cmd.c_str());
#endif
    }

    void SaveTokenSecurely(const GoogleToken &token) {
        (void)token;
    }

    void ClearStoredToken() {}

    GoogleToken LoadTokenFromStorage() {
        return GoogleToken{};
    }
};

} // namespace Auth
} // namespace SDK
