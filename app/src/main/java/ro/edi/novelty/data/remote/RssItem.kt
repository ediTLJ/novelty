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
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.Root
import java.io.Serializable

@Root(name = "item", strict = false)
data class RssItem(
    @field:Element(name = "title", required = false)
    @param:Element(name = "title", required = false)
    val title: String? = null,

    @field:Element(name = "link", required = false)
    @param:Element(name = "link", required = false)
    val link: String? = null,

    @field:Element(name = "description", required = false)
    @param:Element(name = "description", required = false)
    val description: String? = null,

    @Namespace(prefix = "dc", reference = "http://purl.org/dc/elements/1.1/")
    @field:Element(name = "creator", required = false)
    @param:Element(name = "creator", required = false)
    val author: String? = null,

    @field:Element(name = "guid", required = false)
    @param:Element(name = "guid", required = false)
    val id: String? = null,

    @field:Element(name = "pubDate", required = false)
    @param:Element(name = "pubDate", required = false)
    val pubDate: String? = null
) : Serializable