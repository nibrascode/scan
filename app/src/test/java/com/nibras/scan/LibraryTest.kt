package com.nibras.scan

import com.nibras.scan.util.Library
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LibraryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun pdf(dir: File, name: String) = File(dir, "$name.pdf").apply { parentFile.mkdirs(); writeText("%PDF") }

    @Test fun foldersCreateRenameDeleteKeepsDocs() {
        val root = tmp.newFolder("docs")
        val f = Library.createFolder(root, "  Invoices/2026 ")!!
        assertTrue(f.isDirectory)
        assertEquals(listOf(f.name), Library.folders(root).map { it.name })
        val d = pdf(f, "a")
        Library.ocrFile(d).writeText("hello")
        val renamed = Library.renameFolder(f, "Bills")!!
        assertEquals("Bills", renamed.name)
        Library.deleteFolderKeepDocs(root, renamed)
        assertFalse(renamed.exists())
        assertTrue(File(root, "a.pdf").exists())
        assertEquals("hello", Library.ocrText(File(root, "a.pdf")))
    }

    @Test fun moveCarriesSidecarsAndAvoidsOverwrite() {
        val root = tmp.newFolder("docs")
        val folder = Library.createFolder(root, "Work")!!
        pdf(folder, "x")
        val src = pdf(root, "x")
        Library.ocrFile(src).writeText("text")
        val moved = Library.move(src, folder)
        assertEquals("x (2).pdf", moved.name)
        assertFalse(src.exists())
        assertEquals("text", Library.ocrText(moved))
        assertEquals(2, Library.allPdfs(root).size)
    }

    @Test fun searchMatchesNameAndOcrTextIgnoringCaseAndDiacritics() {
        assertTrue(Library.matches("Müqavilə 2026", "", "muqavile"))
        assertTrue(Library.matches("scan_1", "Bakı şəhəri, Nizami küçəsi", "BAKI"))
        assertTrue(Library.matches("scan_1", "Договор аренды", "договор"))
        assertTrue(Library.matches("x", "بسم الله", "الله"))
        assertFalse(Library.matches("scan_1", "abc", "xyz"))
        assertTrue(Library.matches("anything", "", "  "))
    }

    @Test fun sortModes() {
        data class D(val n: String, val t: Long, val s: Long)
        val items = listOf(D("b", 2, 10), D("A", 1, 30), D("c", 3, 20))
        fun names(s: Library.Sort) = Library.sort(items, s, { it.n }, { it.t }, { it.s }).map { it.n }
        assertEquals(listOf("c", "b", "A"), names(Library.Sort.DATE_DESC))
        assertEquals(listOf("A", "b", "c"), names(Library.Sort.DATE_ASC))
        assertEquals(listOf("A", "b", "c"), names(Library.Sort.NAME_ASC))
        assertEquals(listOf("c", "b", "A"), names(Library.Sort.NAME_DESC))
        assertEquals(listOf("A", "c", "b"), names(Library.Sort.SIZE_DESC))
    }

    @Test fun invalidNamesRejected() {
        val root = tmp.newFolder("docs")
        assertNull(Library.createFolder(root, "   "))
    }
}
