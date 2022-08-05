/*
 * Copyright © 2014-2021 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */

package app.passwordstore.injection.pwgen

import android.content.Context
import app.passwordstore.passgen.diceware.DicewarePassphraseGenerator
import app.passwordstore.passgen.diceware.Die
import app.passwordstore.passgen.diceware.RandomIntGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import java.security.SecureRandom
import javax.inject.Qualifier

@Module
@InstallIn(FragmentComponent::class)
object DicewareModule {
  private const val DEFAULT_SIDES = 6

  @Provides
  fun provideDicewareGenerator(
    die: Die,
    @WordlistQualifier wordList: InputStream,
  ): DicewarePassphraseGenerator {
    return DicewarePassphraseGenerator(die, wordList)
  }

  @Provides
  fun provideDie(
    intGenerator: RandomIntGenerator,
  ): Die {
    return Die(DEFAULT_SIDES, intGenerator)
  }

  @Provides
  fun provideRandomIntGenerator(): RandomIntGenerator {
    return RandomIntGenerator { range ->
      SecureRandom().nextInt(range.last).coerceAtLeast(range.first)
    }
  }

  @[Provides WordlistQualifier]
  fun provideDefaultWordList(@ApplicationContext context: Context): InputStream {
    return context.resources.openRawResource(
      app.passwordstore.passgen.diceware.R.raw.diceware_wordlist
    )
  }
}

@Qualifier annotation class WordlistQualifier
