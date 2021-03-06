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

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Root
import org.simpleframework.xml.Text
import java.io.Serializable

@Root(name = "link", strict = false)
data class AtomLink(
    @param:Attribute(name = "href", required = false)
    @field:Attribute(name = "href", required = false)
    val href: String? = null,

    @param:Attribute(name = "rel", required = false)
    @field:Attribute(name = "rel", required = false)
    val rel: String? = null,

    @param:Text(required = false)
    @field:Text(required = false)
    val value: String? = null
) : Serializable