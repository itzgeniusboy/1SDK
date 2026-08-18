// ============================================================================
// USAGE EXAMPLE - BGMI Google Login with Virtual Blackbox SDK
// ============================================================================

#include "SDK/Auth/GoogleLoginProxy.hpp"
#include "SDK/Auth/GoogleAuthManager.hpp"
#include "SDK/Core/Memory.hpp"
#include <iostream>
#include <string>
#include <functional>
#include <cstdio>

using namespace SDK::Auth;

void Example_AsyncLogin() {
    auto &auth = GoogleLoginProxy::Get();
    std::string device_id = "your_device_id_here";

    bool started = auth.Login(
        device_id,
        [](const GoogleAuthToken &token) {
            std::cout << "Login Successful!\n";
            std::cout << "Access Token: " << token.access_token.substr(0, 20) << "...\n";
            std::cout << "Expires In: " << token.expires_in << " seconds\n";
        },
        [](const std::string &error) { std::cout << "Login Failed: " << error << "\n"; },
        [](ELoginStatus, const std::string &message) { std::cout << "Status: " << message << "\n"; });

    if (!started) {
        std::cout << "Failed to start login flow\n";
    }
}

void Example_BlockingLogin() {
    auto &auth = GoogleLoginProxy::Get();

    GoogleAuthToken token;
    bool success = auth.LoginBlocking("device_id", 120000, token);

    if (success) {
        std::cout << "Token: " << token.access_token << "\n";
    } else {
        std::cout << "Login failed or timeout\n";
    }
}

class BGMIAuthManager {
public:
    void Initialize() {
        if (LoadSavedToken()) {
            if (m_token.IsValid()) {
                InjectTokenIntoGame();
                return;
            }

            if (RefreshToken()) {
                InjectTokenIntoGame();
                return;
            }
        }

        StartGoogleLogin();
    }

private:
    GoogleAuthToken m_token;

    bool LoadSavedToken() { return false; }
    void SaveToken() {}

    bool RefreshToken() {
        auto &auth = GoogleLoginProxy::Get();
        return auth.RefreshToken(m_token);
    }

    void StartGoogleLogin() {
        auto &auth = GoogleLoginProxy::Get();

        auth.Login(GetDeviceId(),
                   [this](const GoogleAuthToken &token) {
                       m_token = token;
                       SaveToken();
                       InjectTokenIntoGame();
                   },
                   [this](const std::string &error) { ShowError("Google Login Failed: " + error); },
                   [](ELoginStatus, const std::string &msg) { UpdateUIStatus(msg); });
    }

    void InjectTokenIntoGame() {
        // Integration placeholder for game-side token usage.
    }

    std::string GetDeviceId() {
        return "device_" + std::to_string(std::hash<std::string>{}("unique_id"));
    }

    void ShowError(const std::string &) {}
    static void UpdateUIStatus(const std::string &) {}
};

static std::string GetVirtualDeviceId() {
    return "virtual_device";
}

static void InjectTokenVirtual(const GoogleAuthToken &) {}
static void HandleVirtualLoginError(const std::string &) {}
static void DirectGoogleLogin() {}

void Example_VirtualModeLogin() {
    bool is_virtual = SDK::Memory::IsVirtualMode();

    if (is_virtual) {
        auto &auth = GoogleLoginProxy::Get();

        auth.Login(GetVirtualDeviceId(),
                   [](const GoogleAuthToken &token) { InjectTokenVirtual(token); },
                   [](const std::string &error) { HandleVirtualLoginError(error); });
    } else {
        DirectGoogleLogin();
    }
}

void Example_SafeManualTokenFlow() {
    auto &auth = SDK::Auth::GoogleAuthManager::Get();

    auth.StartLoginFlow(
        [](const SDK::Auth::GoogleToken &token) {
            std::cout << "Token stored safely. Ready for API use.\n";
            std::cout << "Access Token Prefix: " << token.access_token.substr(0, 12) << "...\n";
        },
        [](const std::string &error) { std::cout << "Auth error: " << error << "\n"; },
        [](SDK::Auth::EAuthStatus, const std::string &message) {
            std::cout << "Status: " << message << "\n";
        });

    // After the user logs in from browser and copies token, collect input from your UI.
    auth.SubmitToken("paste_access_token_here", "optional_refresh_token", 3600);
}


struct HttpRequest {
    void AddHeader(const std::string &, const std::string &) {}
    void SetUrl(const std::string &) {}
    void Send() {}
};

void OnSDKInitialize() {
    auto &auth = SDK::Auth::GoogleAuthManager::Get();

    // Check if already logged in
    const auto &token = auth.GetToken();
    if (token.IsValid()) {
        // Already have token, use it
        return;
    }

    // Start login flow
    auth.StartLoginFlow(
        [](const SDK::Auth::GoogleToken &t) {
            printf("Login successful! Token: %s...\n", t.access_token.substr(0, 10).c_str());
        },
        [](const std::string &error) { printf("Login failed: %s\n", error.c_str()); },
        [](SDK::Auth::EAuthStatus, const std::string &msg) { printf("Status: %s\n", msg.c_str()); });
}

void OnUserPastedToken(const std::string &pasted_token) {
    auto &auth = SDK::Auth::GoogleAuthManager::Get();
    auth.SubmitToken(pasted_token, "", 3600);
}

void CallSomeAPI() {
    auto &auth = SDK::Auth::GoogleAuthManager::Get();

    if (!auth.GetToken().IsValid()) {
        printf("Please login first!\n");
        return;
    }

    HttpRequest req;
    auth.BuildAuthenticatedRequest(req);
    req.SetUrl("https://api.example.com/data");
    req.Send();
}
