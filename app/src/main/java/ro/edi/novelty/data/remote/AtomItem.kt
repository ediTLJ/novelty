/*
* Copyright 2019 Eduard Scarlat
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
package ro.edi.novelty.data.remote

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import java.io.Serializable

@Root(name = "entry", strict = false)
data class AtomItem(
    @field:Element(name = "id")
    @param:Element(name = "id")
    val id: String,

    @field:Element(name = "title")
    @param:Element(name = "title")
    val title: String,

    @field:Element(name = "updated")
    @param:Element(name = "updated")
    val updatedDate: String,

    @field:Element(name = "published", required = false)
    @param:Element(name = "published", required = false)
    val pubDate: String? = null,

    @field:Element(name = "summary", required = false)
    @param:Element(name = "summary", required = false)
    val summary: String? = null,

    @field:Element(name = "content", required = false)
    @param:Element(name = "content", required = false)
    val content: String? = null,

    @field:Element(name = "link", required = false)
    @param:Element(name = "link", required = false)
    val link: AtomLink? = null,

    @field:Element(name = "author", required = false)
    @param:Element(name = "author", required = false)
    val author: AtomAuthor? = null
) : Serializable