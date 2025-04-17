package com.aprz.gdsaveeditor

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aprz.gdsaveeditor.DiySkills.decryptSkill

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.aprz.gdsaveeditor", appContext.packageName)

        println(

        DiySkills.decryptSkill(
            "353039656134666264393838663635333161383436653632336265323163363331313032303065623836656161313664363963616238363564353335613739363032363037646665343666326130663962303563356234633937623239373261353464343235323435646662616561353330383832393564643366373465316361386164643165313365386136666534303465393334316335386162636538366461333438353238633532373937376665613863343963303335643339396135653438383834383863633634303333623533326562366439663434323561356131346138313234633239636432623036613865333135313930313132666336632252ddd14d4b03ee0a8dab8ad8aa8b59e2ac18e8198ddface37139ec62cd54884793ece3916cd223d6da46a92a360e616e9566871ad903875734bb6da5f695d34002"
        ).toString()
        )
    }
}