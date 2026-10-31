package com.nibras.scan

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Every string in the default (az) file must exist in en, tr, ru and ar. */
class StringsCompletenessTest {
    private fun keys(dir: String): Set<String> {
        val candidates = listOf(File("src/main/res/$dir/strings.xml"), File("app/src/main/res/$dir/strings.xml"))
        val f = candidates.first { it.exists() }
        return Regex("<string name=\"([^\"]+)\"").findAll(f.readText()).map { it.groupValues[1] }.toSet()
    }

    @Test fun allLocalesComplete() {
        val base = keys("values")
        for (l in listOf("values-en", "values-tr", "values-ru", "values-ar")) {
            val missing = base - keys(l)
            assertTrue("$l missing $missing", missing.isEmpty())
        }
    }
}
