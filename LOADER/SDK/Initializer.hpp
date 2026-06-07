#pragma once

#include "Core/Memory.hpp"
#include "Anogs/AnogsWrapper.hpp"
#include "../anogs.h"

namespace SDK {

    class Initializer {
    public:
        static bool Init(bool bVirtualMode = false) {
            Memory::SetMode(bVirtualMode ? Memory::EMode::Virtual : Memory::EMode::Normal);
            Anogs::OffsetResolver::RefreshBase();

            if (!VerifyOffsets()) {
                return false;
            }

            return InitializeCore();
        }

    private:
        static bool VerifyOffsets() {
            auto gobj = ::Anogs::GetGObjects();
            if (gobj == 0) return false;

            if (Memory::IsVirtualMode()) {
                return Memory::Virtual::IsValidPointer((void *) gobj);
            }

            return true;
        }

        static bool InitializeCore() {
            return true;
        }
    };

} // namespace SDK
