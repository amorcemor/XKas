package com.mibi.xkas.data.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.mibi.xkas.data.model.Contact
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val TAG = "ContactRepository"

    suspend fun getAllContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Contact>()
        val contentResolver: ContentResolver = context.contentResolver

        try {
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameColumn = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneColumn = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val cid = it.getString(idColumn)
                    val name = it.getString(nameColumn) ?: ""
                    val phone = it.getString(phoneColumn) ?: ""

                    // Bersihkan nomor telepon dari karakter non-digit kecuali +
                    val cleanPhone = phone.replace(Regex("[^+\\d]"), "")

                    if (name.isNotBlank() && cleanPhone.isNotBlank()) {
                        contacts.add(Contact(contactId = cid, name = name, phoneNumber = cleanPhone))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading contacts", e)
        }

        // Remove duplicates berdasarkan nama dan nomor
        contacts.distinctBy { "${it.name}_${it.phoneNumber}" }
    }

    suspend fun searchContacts(query: String): List<Contact> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getAllContacts()

        getAllContacts().filter { contact ->
            contact.name.contains(query, ignoreCase = true) ||
                    contact.phoneNumber.contains(query)
        }
    }
    suspend fun saveContact(contact: Contact): Result<Contact> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Untuk implementasi sederhana, return contact dengan ID yang dihasilkan
            val newContact = contact.copy(
                contactId = if (contact.contactId.isBlank())
                    java.util.UUID.randomUUID().toString()
                else contact.contactId
            )
            Result.success(newContact)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving contact", e)
            Result.failure(e)
        }
    }
}
