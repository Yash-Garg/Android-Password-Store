/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.passwordstore.util.totp

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.runCatching
import java.nio.ByteBuffer
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and
import org.apache.commons.codec.binary.Base32

internal object Otp {

  private val BASE_32 = Base32()
  private val STEAM_ALPHABET = "23456789BCDFGHJKMNPQRTVWXY".toCharArray()

  fun calculateCode(
    secret: String,
    counter: Long,
    algorithm: String,
    digits: String,
    issuer: String?,
  ) = runCatching {
    val algo = "Hmac${algorithm.uppercase(Locale.ROOT)}"
    val decodedSecret = BASE_32.decode(secret)
    val secretKey = SecretKeySpec(decodedSecret, algo)
    val digest =
      Mac.getInstance(algo).run {
        init(secretKey)
        doFinal(ByteBuffer.allocate(8).putLong(counter).array())
      }
    // Least significant 4 bits are used as an offset into the digest.
    val offset = (digest.last() and 0xf).toInt()
    // Extract 32 bits at the offset and clear the most significant bit.
    val code = digest.copyOfRange(offset, offset + 4)
    code[0] = (0x7f and code[0].toInt()).toByte()
    val codeInt = ByteBuffer.wrap(code).int
    check(codeInt > 0)
    // SteamGuard is a horrible OTP implementation that generates non-standard 5 digit OTPs as
    // well
    // as uses a custom character set.
    if (digits == "s" || issuer == "Steam") {
      var remainingCodeInt = codeInt
      buildString {
        repeat(5) {
          append(STEAM_ALPHABET[remainingCodeInt % STEAM_ALPHABET.size])
          remainingCodeInt /= 26
        }
      }
    } else {
      // Base 10, 6 to 10 digits
      val numDigits = digits.toIntOrNull()
      when {
        numDigits == null -> {
          return Err(IllegalArgumentException("Digits specifier has to be either 's' or numeric"))
        }
        numDigits < 6 -> {
          return Err(IllegalArgumentException("TOTP codes have to be at least 6 digits long"))
        }
        numDigits > 10 -> {
          return Err(IllegalArgumentException("TOTP codes can be at most 10 digits long"))
        }
        else -> {
          // 2^31 = 2_147_483_648, so we can extract at most 10 digits with the first one
          // always being 0, 1, or 2. Pad with leading zeroes.
          val codeStringBase10 = codeInt.toString(10).padStart(10, '0')
          check(codeStringBase10.length == 10)
          codeStringBase10.takeLast(numDigits)
        }
      }
    }
  }
}
