#ifndef KEEPALIVE_STRING16_H
#define KEEPALIVE_STRING16_H

#include <stdint.h>
#include <string.h>
#include "Unicode.h"

namespace android {
    class String16 {
    public:
        String16();

        explicit String16(const char *o);

        explicit String16(const Char16 *o, size_t len);

        ~String16();

        inline const Char16 *string() const;

        size_t size() const;

    private:
        const Char16 *mString;
    };

    inline const Char16 *String16::string() const {
        return mString;
    }
}
#endif //KEEPALIVE_STRING16_H
