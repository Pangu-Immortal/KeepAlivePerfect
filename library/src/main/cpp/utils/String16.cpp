#include "String16.h"
#include "SharedBuffer.h"

namespace android {
    static Char16 *gEmptyString = NULL;

    static inline Char16 *getEmptyString() {
        return gEmptyString;
    }

    static Char16 *allocFromUTF8(const char *u8str, size_t u8len) {
        if (u8len == 0) return getEmptyString();

        const uint8_t *u8cur = (const uint8_t *) u8str;

        const ssize_t u16len = utf8_to_utf16_length(u8cur, u8len);
        if (u16len < 0) {
            return getEmptyString();
        }

        SharedBuffer *buf = SharedBuffer::alloc(sizeof(Char16) * (u16len + 1));
        if (buf) {
            u8cur = (const uint8_t *) u8str;
            Char16 *u16str = (Char16 *) buf->data();

            utf8_to_utf16(u8cur, u8len, u16str, ((size_t) u16len) + 1);

            //printf("Created UTF-16 string from UTF-8 \"%s\":", in);
            //printHexData(1, str, buf->size(), 16, 1);
            //printf("\n");

            return u16str;
        }

        return getEmptyString();
    }

    static Char16 *allocFromUTF16(const Char16 *u16str, size_t u16len) {
        if (u16len >= SIZE_MAX / sizeof(Char16)) {
//            android_errorWriteLog(0x534e4554, "73826242");
//            abort();
            return NULL;
        }

        SharedBuffer *buf = SharedBuffer::alloc((u16len + 1) * sizeof(Char16));
//        ALOG_ASSERT(buf, "Unable to allocate shared buffer");
        if (buf) {
            Char16 *str = (Char16 *) buf->data();
            memcpy(str, u16str, u16len * sizeof(Char16));
            str[u16len] = 0;
            return str;
        }
        return getEmptyString();
    }

    String16::String16() : mString(getEmptyString()) {}

    String16::String16(const char *o) : mString(allocFromUTF8(o, strlen(o))) {}

    String16::String16(const Char16 *o, size_t len) : mString(allocFromUTF16(o, len)) {}

    size_t String16::size() const {
        return SharedBuffer::sizeFromData(mString) / sizeof(Char16) - 1;
    }

    String16::~String16() {
        SharedBuffer::bufferFromData(mString)->release();
    }
}