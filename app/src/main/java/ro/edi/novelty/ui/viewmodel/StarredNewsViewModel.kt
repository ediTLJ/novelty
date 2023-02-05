/*
* Copyright 2023 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package ro.edi.novelty.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ro.edi.novelty.data.DataManager
import ro.edi.novelty.model.News

class StarredNewsViewModel(application: Application, savedStateHandle: SavedStateHandle) :
    NewsViewModel(application, savedStateHandle) {

    override val news: LiveData<List<News>> by lazy(LazyThreadSafetyMode.NONE) {
        DataManager.getInstance(application).getMyNews()
    }

    companion object {
        val FACTORY = viewModelFactory {
            // the return type of the lambda automatically sets what class this lambda handles
            initializer {
                // get the Application object from extras provided to the lambda
                val application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])

                val savedStateHandle = createSavedStateHandle()

                StarredNewsViewModel(
                    application = application,
                    savedStateHandle = savedStateHandle
                )
            }
        }
    }
}