#pragma once

#include <cstdint>
#include "Memory.hpp"
#include "../../anogs.h"

namespace SDK {

    struct TUObjectArray;

    class UObject {
    public:
        static TUObjectArray *GetGlobalObjects() {
            void *ptr = Anogs::Ptr<void>(Anogs::GObjectsOffset);
            return Memory::Read<TUObjectArray *>(ptr);
        }

        template<typename T>
        T GetProperty(uintptr_t offset) {
            return Memory::Read<T>((void *) ((uintptr_t) this + offset));
        }

        template<typename T>
        void SetProperty(uintptr_t offset, T value) {
            Memory::Write<T>((void *) ((uintptr_t) this + offset), value);
        }
    };

} // namespace SDK
