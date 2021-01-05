package com.example.myapplication

import android.util.Log
import android.widget.Toast
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature
import com.microsoft.azure.storage.StorageUri
import com.microsoft.azure.storage.blob.CloudBlobClient
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlockBlob
import kotlinx.coroutines.GlobalScope
import org.jetbrains.anko.doAsync
import java.io.File


fun uploadImagesToAzure(file: File, azureContainer: CloudBlobContainer){
    GlobalScope.doAsync {
        try {
            val blob: CloudBlockBlob = azureContainer.getBlockBlobReference(file.name)
            //Toast.makeText(this, path, Toast.LENGTH_SHORT).show();
            blob.uploadFromFile(file.absolutePath)
        } catch (e: Exception) {
            Log.v("LXT", e.message)
        }
    }
}



fun setupAzure(sasToken: String, uriStorage: StorageUri){
    GlobalScope.doAsync {
        try {
            val accountSAS = StorageCredentialsSharedAccessSignature(sasToken)
            val blobClient = CloudBlobClient(uriStorage, accountSAS)
            val azureContainer = blobClient.getContainerReference("intel-images2")
            azureContainer.createIfNotExists()
        } catch (e: Throwable) {
            Log.v("LXT", "other")
        }
    }
}