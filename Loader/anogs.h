#pragma once

#include <cstdint>
#include "SDK/Anogs/AnogsWrapper.hpp"

namespace Anogs {
    // ========== RELATIVE OFFSETS (Not Absolute) ==========
    // Hamesha relative offsets rakho, absolute nahi
    constexpr uintptr_t GObjectsOffset = 0x39371A0; // Relative from base
    constexpr uintptr_t GNamesOffset   = 0x392E310; // Relative from base
    constexpr uintptr_t WorldOffset    = 0x0;       // TODO: set real relative offset
    constexpr uintptr_t SomeCountOffset = 0x0;    // TODO: set real relative offset

    // ========== RESOLVED POINTERS (Auto mode handle) ==========
    inline uintptr_t GetGObjects() {
        return SDK::Anogs::OffsetResolver::Resolve(GObjectsOffset);
    }

    inline uintptr_t GetGNames() {
        return SDK::Anogs::OffsetResolver::Resolve(GNamesOffset);
    }

    // ========== DIRECT ACCESS HELPERS ==========
    // Virtual mode mein bhi kaam karega
    template<typename T>
    inline T Read(uintptr_t relativeOffset) {
        return SDK::Memory::Read<T>((void *) SDK::Anogs::OffsetResolver::Resolve(relativeOffset));
    }

    template<typename T>
    inline void Write(uintptr_t relativeOffset, T value) {
        SDK::Memory::Write<T>((void *) SDK::Anogs::OffsetResolver::Resolve(relativeOffset), value);
    }

    template<typename T>
    inline T *Ptr(uintptr_t relativeOffset) {
        return SDK::Memory::Ptr<T>(SDK::Anogs::OffsetResolver::Resolve(relativeOffset));
    }

} // namespace Anogs
