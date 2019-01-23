package de.spover.spover.network

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "osm")
data class Osm(
        // should be optional
        @JacksonXmlProperty(localName = "node", isAttribute = false)
        @JacksonXmlElementWrapper(useWrapping = false)
        val nodes: List<OsmNode>?,

        @JacksonXmlProperty(localName = "way", isAttribute = false)
        @JacksonXmlElementWrapper(useWrapping = false)
        val ways: List<OsmWay>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OsmNode(
        val id: String,
        val lat: String,
        val lon: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OsmWay(
        val id: String,

        @JacksonXmlProperty(localName = "nd", isAttribute = false)
        @JacksonXmlElementWrapper(useWrapping = false)
        val nodeRefs: List<OsmNodeRef>,

        @JacksonXmlProperty(localName = "tag", isAttribute = false)
        @JacksonXmlElementWrapper(useWrapping = false)
        val tags: List<OsmTag>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OsmNodeRef(
        val ref: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OsmTag(
        val k: String,
        val v: String
)
