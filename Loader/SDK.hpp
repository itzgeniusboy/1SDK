#pragma once

#include "SDK/Core/Memory.hpp"
#include "SDK/Anogs/AnogsWrapper.hpp"
#include "SDK/Core/UObject.hpp"
#include "SDK/Initializer.hpp"
#include "SDK/Auth/GoogleAuthManager.hpp"
#include "anogs.h"

namespace SDK {
    // Optional integration point for runtime virtual-mode detection.
    // Replace implementation based on host loader/environment.
    inline bool DetectVirtualMode() {
        return false;
    }
}
