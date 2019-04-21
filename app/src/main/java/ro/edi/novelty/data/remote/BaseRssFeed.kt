package ro.edi.novelty.data.remote

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import java.io.Serializable

@Root(name = "rss", strict = false)
data class BaseRssFeed(
        @field:Element(name = "channel")
        @param:Element(name = "channel")
        val channel: RssChannel
) : Serializable