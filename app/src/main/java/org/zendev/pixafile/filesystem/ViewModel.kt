package org.zendev.pixafile.filesystem

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.zendev.pixafile.filesystem.models.ZFile

class ViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DeviceRepository(application)

    fun getFiles(path: String) : MutableLiveData<MutableList<ZFile>> {
        return repository.getFiles(path)
    }
}