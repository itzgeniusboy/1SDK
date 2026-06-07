#pragma once

#include <cstdint>
#include <cstdio>
#include "Core/Memory.hpp"

namespace SDK {
namespace Anogs {

    class OffsetResolver {
    private:
        static uintptr_t &BaseRef() {
            static uintptr_t base = 0;
            return base;
        }

        static uintptr_t GetNativeBase() {
            uintptr_t base = 0;
            char line[512] = {0};
            FILE *fp = fopen("/proc/self/maps", "rt");
            if (!fp) return 0;

            while (fgets(line, sizeof(line), fp)) {
                uintptr_t start = 0;
                if (sscanf(line, "%lx-%*lx", &start) == 1) {
                    base = start;
                    break;
                }
            }
            fclose(fp);
            return base;
        }

    public:
        static uintptr_t GetBase() {
            auto &base = BaseRef();
            if (base == 0) {
                if (Memory::IsVirtualMode()) {
                    base = Memory::Virtual::GetModuleBase(nullptr);
                } else {
                    base = GetNativeBase();
                }
            }
            return base;
        }

        static void RefreshBase() {
            BaseRef() = 0;
        }

        static uintptr_t Resolve(uintptr_t relativeOffset) {
            return GetBase() + relativeOffset;
        }

        template<typename T>
        static T *ResolvePtr(uintptr_t relativeOffset) {
            return Memory::Ptr<T>(Resolve(relativeOffset));
        }
    };

#define ANOGS_RESOLVE(offset) SDK::Anogs::OffsetResolver::Resolve(offset)
#define ANOGS_PTR(type, offset) SDK::Anogs::OffsetResolver::ResolvePtr<type>(offset)
#define ANOGS_READ(type, offset) SDK::Memory::Read<type>((void *) ANOGS_RESOLVE(offset))
#define ANOGS_WRITE(type, offset, value) SDK::Memory::Write<type>((void *) ANOGS_RESOLVE(offset), value)

} // namespace Anogs
} // namespace SDK
