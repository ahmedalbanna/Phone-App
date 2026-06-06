package com.example.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.model.ContactItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val context: Context) {

    // Prepopulated beautiful Arabic contacts as a fallback or merged
    private val mockContacts = listOf(
        ContactItem("أحمد البنا", "0501234567", "ahmed.banna@gmail.com", "أ"),
        ContactItem("أمل الحربي", "0559876543", "amal.harbi@outlook.com", "أ"),
        ContactItem("ياسين التونسي", "0564567890", "yassin.tounsi@icloud.com", "ي"),
        ContactItem("خالد العتيبي", "0543210987", "khaled.otaibi@ksub.edu.sa", "خ"),
        ContactItem("سارة الشمري", "0531112222", "sara.shammari@gmail.com", "س"),
        ContactItem("مريم القحطاني", "0548889999", "mariam.qahtani@gov.sa", "م"),
        ContactItem("عمر الفاروق", "0552223333", "omar.farooq@company.com", "ع"),
        ContactItem("ليلى الهاشمي", "0561239876", "laila.hashemi@gmail.com", "ل"),
        ContactItem("كريم المصري", "0505556666", "karim.masri@hotmail.com", "ك"),
        ContactItem("فاطمة الزهراء", "0547774444", "fatima.zahra@gmail.com", "ف")
    )

    suspend fun getContacts(query: String = ""): List<ContactItem> = withContext(Dispatchers.IO) {
        val systemContacts = mutableListOf<ContactItem>()
        
        val hasPermission = ContextCompat.checkSelfPermission(
            context, 
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                val cursor: Cursor? = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                    ),
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )

                cursor?.use {
                    val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    while (it.moveToNext()) {
                        val name = if (nameIdx != -1) it.getString(nameIdx) ?: "اسم مجهول" else "اسم مجهول"
                        val number = if (numberIdx != -1) it.getString(numberIdx) ?: "" else ""
                        
                        if (number.isNotEmpty() && systemContacts.none { c -> c.phoneNumber == number }) {
                            val initials = if (name.isNotEmpty()) name.take(1) else "؟"
                            systemContacts.add(ContactItem(name, number, initials = initials))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // If system contacts is empty, use our beautiful pre-populated list
        val baseList = if (systemContacts.isEmpty()) mockContacts else systemContacts

        if (query.isEmpty()) {
            return@withContext baseList.sortedBy { it.name }
        } else {
            return@withContext baseList.filter {
                it.name.contains(query, ignoreCase = true) || 
                it.phoneNumber.contains(query)
            }.sortedBy { it.name }
        }
    }
}
