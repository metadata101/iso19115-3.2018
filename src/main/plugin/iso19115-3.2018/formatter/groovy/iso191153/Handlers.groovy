/*
 * Copyright (C) 2001-2016 Food and Agriculture Organization of the
 * United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * and United Nations Environment Programme (UNEP)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 * Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * Rome - Italy. email: geonetwork@osgeo.org
 */

package iso19115_3

import org.fao.geonet.api.records.formatters.groovy.Environment
import org.fao.geonet.api.records.formatters.groovy.MapConfig

public class Handlers {
    protected org.fao.geonet.api.records.formatters.groovy.Handlers handlers;
    protected org.fao.geonet.api.records.formatters.groovy.Functions f
    protected Environment env
    Matchers matchers
    iso19115_3.Functions isofunc
    common.Handlers commonHandlers
    List<String> packageViews
    String rootEl = 'mdb:MD_Metadata'

    public Handlers(handlers, f, env) {
        this.handlers = handlers
        this.f = f
        this.env = env
        commonHandlers = new common.Handlers(handlers, f, env)
        isofunc = new iso19115_3.Functions(handlers: handlers, f:f, env:env, commonHandlers: commonHandlers)
        matchers =  new Matchers(handlers: handlers, f:f, env:env)
        packageViews = [
                'mdb:identificationInfo', 'mdb:metadataMaintenance', 'mdb:metadataConstraints', 'mdb:spatialRepresentationInfo',
                'mdb:distributionInfo', 'mdb:applicationSchemaInfo', 'mdb:dataQualityInfo', 'mdb:portrayalCatalogueInfo',
                'mdb:contentInfo', 'mdb:metadataExtensionInfo', 'mdb:referenceSystemInfo', rootEl]
    }

    def addDefaultHandlers() {
        handlers.add name: 'Text Elements', select: matchers.isTextEl, isoTextEl
        handlers.add name: 'Simple Text Elements', select: matchers.isSimpleTextEl, isoSimpleTextEl
        handlers.add name: 'URL Elements', select: matchers.isTextEl, isoTextEl
        handlers.add name: 'Anchor URL Elements', select: matchers.isAnchorUrlEl, isoAnchorUrlEl
        handlers.add name: 'Simple Elements', select: matchers.isBasicType, isoBasicType
        handlers.add name: 'Boolean Elements', select: matchers.isBooleanEl, isoBooleanEl
        handlers.add name: 'CodeList Elements', select: matchers.isCodeListEl, isoCodeListEl
        handlers.add name: 'Date Elements', select: matchers.isDateEl, dateEl
        handlers.add name: 'Format Elements',  select: matchers.isFormatEl, group: true, formatEls
        handlers.add name: 'Keyword Elements', select: 'mri:descriptiveKeywords', group:true, keywordsEl
        handlers.add name: 'ResponsibleParty Elements', select: matchers.isRespParty, pointOfContactEl
        handlers.add name: 'Graphic Overview', select: 'mri:graphicOverview', group: true, graphicOverviewEl
        handlers.add select: 'lan:language', group: false, isoLanguageEl
        handlers.add select: matchers.isCiOnlineResourceParent, group: true, onlineResourceEls
        handlers.add select: 'srv:coupledResource', group: true, coupledResourceEls
        handlers.add select: 'srv:containsOperations', group: true, containsOperationsEls
        handlers.add name: 'mri:topicCategory', select: 'mri:topicCategory', group: true, { elems ->
            def listItems = elems.findAll{!it.text().isEmpty()}.collect {f.codelistValueLabel("MD_TopicCategoryCode", it.text())};
            handlers.fileResult("html/list-entry.html", [label:f.nodeLabel(elems[0]), listItems: listItems])
        }

        handlers.skip name: "skip date parent element", select: matchers.hasDateChild, {it.children()}
        handlers.skip name: "skip codelist parent element", select: matchers.hasCodeListChild, {it.children()}
        handlers.skip name: "skip containers: " + matchers.skipContainers, select: matchers.isSkippedContainer, {it.children()}

        handlers.add select: 'mdb:otherLocale', group: true, localeEls
        handlers.add 'cit:CI_Date', ciDateEl
        handlers.add 'cit:CI_Citation', citationEl
        handlers.add name: 'Root Element', select: matchers.isRoot, rootPackageEl

        handlers.add name: 'identificationInfo elements', select: {it.parent().name() == 'mdb:identificationInfo'}, commonHandlers.entryEl(f.&nodeLabel, {el -> 'mdb_identificationInfo'})
        handlers.add name: 'Container Elements', select: matchers.isContainerEl, priority: -1, commonHandlers.entryEl(f.&nodeLabel, addPackageViewClass)

        handlers.add name: 'Contact Organisation Elements', select: 'cit:CI_Organisation', organisationEl
        handlers.add name: 'Contact Individual Elements', select: 'cit:CI_Individual', individualEl
        handlers.add name: 'Time Period Position Elements', select: matchers.isTimePeriodPositionEl, timePeriodPositionEl

        commonHandlers.addDefaultStartAndEndHandlers();
        addExtentHandlers()

        handlers.sort name: 'Text Elements', select: matchers.isContainerEl, priority: -1, sortContainerEl
    }

    def sortContainerEl = {el1, el2 ->
        def v1 = matchers.isContainerEl(el1) ? 1 : -1;
        def v2 = matchers.isContainerEl(el2) ? 1 : -1;
        return v1 - v2
    }
    def addPackageViewClass = {el -> if (packageViews.contains(el.name())) return el.name().replace(':', '_')}

    def addExtentHandlers() {
        handlers.add commonHandlers.matchers.hasChild('gex:EX_Extent'), commonHandlers.flattenedEntryEl({it.'gex:EX_Extent'}, f.&nodeLabel)
        handlers.add name: 'BBox Element', select: matchers.isBBox, bboxEl(false)
        handlers.add name: 'Polygon Element', select: matchers.isPolygon, polygonEl(false)
        handlers.add 'gex:geographicElement', commonHandlers.processChildren{it.children()}
        handlers.add 'gmd:extentTypeCode', extentTypeCodeEl
    }

    def isoTextEl = { isofunc.isoTextEl(it, isofunc.isoText(it))}
    def isoAnchorUrlEl = { isofunc.isoUrlEl(it, isofunc.isoAnchorUrlLink(it), isofunc.isoAnchorUrlText(it))}
    def isoDatasetUriEl = { isofunc.isoUrlEl(it, isofunc.isoText(it), isofunc.isoText(it))}
    def isoCodeListEl = {isofunc.isoTextEl(it, f.codelistValueLabel(it))}
    def isoBasicType = {isofunc.isoTextEl(it, it.'*'.text())}
    def isoSimpleTextEl = { isofunc.isoTextEl(it, it.text()) }
    def isoSimpleTextElGrouped = { elems ->
        def listItems = elems.findAll{!it.text().isEmpty()}.collect {it.text()};
        handlers.fileResult("html/list-entry.html", [label:f.nodeLabel(elems[0]), listItems: listItems])
    }
    def parseBool(text) {
        switch (text.trim().toLowerCase()){
            case "1":
            case "true":
            case "y":
                return true;
            default:
                return false;
        }
    }
    def isoBooleanEl = {isofunc.isoTextEl(it, parseBool(it.'*'.text()).toString())}
    def dateEl = {isofunc.isoTextEl(it, isofunc.dateText(it));}
    def extentTypeCodeEl = {
        isofunc.isoTextEl(it, parseBool(it.text()) ? 'include' : 'excluded')
    }
    def ciDateEl = {
        if(matchers.isDateEl(it.'cit:date')) {
            def dateType = f.codelistValueLabel(it.'cit:dateType'.'cit:CI_DateTypeCode')
            commonHandlers.func.textEl(dateType, isofunc.dateText(it.'cit:date'));
        }
    }
    def localeEls = { els ->
        def locales = []
        els.each {
            it.'lan:PT_Locale'.each { loc ->
                locales << [
                        language: f.codelistValueLabel(loc.'lan:languageCode'.'lan:LanguageCode'),
                        charset: f.codelistValueLabel(loc.'lan:characterEncoding'.'lan:MD_CharacterSetCode')
                ]
            }
        }
        handlers.fileResult("html/locale.html", [
                label: f.nodeLabel(els[0]),
                locales: locales
        ])
    }
    def isoLanguageEl = { language ->
        def lang;
        if (!language.'lan:LanguageCode'.isEmpty()) {
            lang = f.codelistValueLabel(language.'lan:LanguageCode')
        } else {
            lang = f.translateLanguageCode(language.text());
        }

        commonHandlers.func.textEl(f.nodeLabel(language), lang);
    }
    def containsOperationsEls = { els ->
        StringBuilder builder = new StringBuilder();
        els.'*'.each{op ->
            builder.append(handlers.processElements(op));
        }

        return handlers.fileResult('html/2-level-entry.html', [label: f.nodeLabel(els[0]), childData: builder.toString()])
    }

    def onlineResourceEls = { els ->
        def links = []
        els.each {it.'cit:CI_OnlineResource'.each { link ->
            def model = [
                    href : isofunc.clean(isofunc.isoText(link.'cit:linkage')),
                    name : isofunc.clean(isofunc.isoText(link.'cit:name')),
                    desc : isofunc.clean(isofunc.isoText(link.'cit:description'))
            ]
            if (model.href != '' || model.name != '' || model.desc != '') {
                links << model;
            }
        }}

        if (links.isEmpty()) {
            return ''
        } else {
            handlers.fileResult('html/online-resource.html', [
                    label: f.nodeLabel(els[0]),
                    links: links
            ])
        }
    }

    def coupledResourceEls = { els ->
        def resources = com.google.common.collect.ArrayListMultimap.create()

        def resolveResource = { el ->
            def resource = el.'srv:SV_CoupledResource'
            if (resource.isEmpty()) {
                resource = el
            }
            resource
        }

        els.each {el ->
            def resource = resolveResource(el)
            def opName = resource.'srv:operationName'.text()
            def identifier = resource.'srv:identifier'.text()
            def scopedName = resource.'gco:ScopedName'.text()

            def tip, href, cls;
            if (identifier.trim().isEmpty()) {
                href = "javascript:alert('" + this.f.translate("noUuidInLink") + "');"
                tip = this.f.translate("noUuidInLink")
                cls = 'text-muted'
            } else {
                href = env.localizedUrl + 'display#/' + identifier + '/formatters/full_view/'
                tip = href
            }
            def category = opName.trim().isEmpty() ? 'uncategorized' : opName
            resources.put(category, [
                    href : href,
                    tip : tip,
                    name : scopedName.trim().isEmpty() ? identifier : scopedName,
                    class: cls
            ]);
        }

        def label = f.nodeLabel("srv:SV_CoupledResource", null)
        if (!els.isEmpty()) {
            label = f.nodeLabel(els[0])
        }

        def model = [label: label, resources: resources.asMap()]
        handlers.fileResult("html/coupled-resources.html", model)
    }
    def formatEls = { els ->
        def formats = [] as Set

        def resolveFormat = { el ->
            def format = el.'mrd:MD_Format'
            if (format.isEmpty()) {
                format = el
            }
            format
        }

        els.each {el ->
            def format = resolveFormat(el)
            def valueMap = [:]
            format.children().list().each {child ->
                if (child.name().equals("mrd:formatDistributor")) {
                    return;
                }
                String[] parts = child.name().split(":");
                String name;
                if (parts.length == 2) {
                    name = parts[1]
                } else {
                    name = parts[0]
                }

                valueMap.put(name, isofunc.isoText(child))
            }
            def distributor = resolveFormat(el).'mrd:formatDistributor'
                .'mrd:MD_Distributor'.'mrd:distributorContact'.'*'
            if (!distributor.text().isEmpty()) {
                valueMap.put('formatDistributor', handlers.processElements(distributor))
            }

            def specificationTitle = isofunc.isoText(resolveFormat(el).'mrd:formatSpecificationCitation'
                .'cit:CI_Citation'.'cit:title')
            def specificationIdentifier = isofunc.isoText(resolveFormat(el).'mrd:formatSpecificationCitation'
                .'cit:CI_Citation'.'cit:identifier')
            valueMap.put('title', specificationTitle)
            valueMap.put('identifier', specificationIdentifier)

            formats.add(valueMap)
        }

        def label = "format"
        if (!els.isEmpty()) {
            label = f.nodeLabel(els[0])
        }

        def model = [label: label, formats: formats]
        handlers.fileResult("html/format.html", model)
    }
    def keywordsEl = {keywords ->
        def keywordProps = com.google.common.collect.ArrayListMultimap.create()
        keywords.collectNested {it.'**'.findAll{it.name() == 'mri:keyword'}}.flatten().each { k ->
            def thesaurusName = isofunc.isoText(k.parent().'mri:thesaurusName'.'cit:CI_Citation'.'cit:title')

            if (thesaurusName.isEmpty()) {
                def keywordTypeCode = k.parent().'mri:type'.'mri:MD_KeywordTypeCode'
                if (!keywordTypeCode.isEmpty()) {
                    thesaurusName = f.translate("uncategorizedKeywords")
                }
            }

            if (thesaurusName.isEmpty()) {
                thesaurusName = f.translate("noThesaurusName")
            }
            keywordProps.put(thesaurusName, isofunc.isoText(k))
        }

        return handlers.fileResult('html/keyword.html', [
                label : f.nodeLabel("mri:descriptiveKeywords", null),
                keywords: keywordProps.asMap()])
    }
    def graphicOverviewEl = {graphics ->
        def links = []
        graphics.each {it.'mcc:MD_BrowseGraphic'.each { graphic ->
            def img = graphic.'mcc:fileName'.text()
            if (img != null) {
                String thumbnailUrl = img.replace("&", "&amp;")
                links << [
                        src : thumbnailUrl,
                        desc: isofunc.isoText(graphic.'mcc:fileDescription')
                ]
            }

        }}
        handlers.fileResult("html/graphic-overview.html", [
                label: f.nodeLabel(graphics[0]),
                graphics: links
        ])
    }
    def citationEl = { el ->
        Set processedChildren = ['cit:title', 'cit:alternateTitle', 'cit:identifier',
                                 'cit:ISBN', 'cit:ISSN', 'cit:date',
                                 'cit:edition', 'cit:editionDate', 'cit:presentationForm']

        def otherChildren = el.children().findAll { ch -> !processedChildren.contains(ch.name()) }

        def model = [
                title :  handlers.processElements([el.'cit:title']),
                altTitle : handlers.processElements([el.'cit:alternateTitle']),
                date : handlers.processElements(el.'cit:date'.'cit:CI_Date'),
                editionInfo: commonHandlers.func.textEl(el.'cit:edition'.text(),
                  el.'cit:editionDate'.'gco:Date'.text()),
                identifier : isofunc.isoWikiTextEl(el.'cit:identifier',
                  el.'cit:identifier'.'*'.'mcc:code'.join('<br/>')),
                presentationForm : isofunc.isoTextEl(el.'cit:presentationForm',
                  f.codelistValueLabel(el.'cit:presentationForm'.'cit:CI_PresentationFormCode')),
                ISBN : handlers.processElements(el.'cit:ISBN'),
                ISSN : handlers.processElements(el.'cit:ISSN'),
                otherData : handlers.processElements(otherChildren)
        ]
        return handlers.fileResult("html/citation.html", model)
    }

    /**
     * El must be a parent of gmd:CI_ResponsibleParty
     */
    def pointOfContactEl = { el ->

        def responsibility = el.children().find { ch ->
            ch.name() == 'cit:CI_Responsibility' ||
              ch['@gco:isoType'].text() == 'cit:CI_Responsibility'
        }

        def orgs = responsibility.'cit:party'.'cit:CI_Organisation'
        def individuals = responsibility.'cit:party'.'cit:CI_Individual'

        def childData = [
          orgs,
          individuals,
          responsibility.'cit:role',
        ]
        return handlers.fileResult('html/2-level-entry.html', [label: f.nodeLabel(el), childData: handlers.processElements(childData)])
    }

    def organisationEl = { el ->
        def contactInfo = el.'cit:contactInfo'.'cit:CI_Contact'
        def individuals = el.'cit:individual'.'cit:CI_Individual'
        def generalChildren = [
            el.'cit:name',
            contactInfo.'cit:address'.'cit:CI_Address'.'*',
            individuals
        ]
        handlers.fileResult('html/2-level-entry.html', [label: f.nodeLabel(el), childData: handlers.processElements(generalChildren)])
    }

    def individualEl = { el ->
        def contactInfo = el.'cit:contactInfo'.'cit:CI_Contact'
        def generalChildren = [
            el.'cit:name',
            contactInfo.'cit:address'.'cit:CI_Address'.'*'
        ]
        handlers.fileResult('html/2-level-entry.html', [label: f.nodeLabel(el), childData: handlers.processElements(generalChildren)])
    }

    def polygonEl(thumbnail) {
        return { el ->
            MapConfig mapConfig = env.mapConfiguration
            def mapproj = mapConfig.mapproj
            def background = mapConfig.background
            def width = thumbnail? mapConfig.thumbnailWidth : mapConfig.width
            def mdId = env.getMetadataId();
            def xpath = f.getXPathFrom(el);

            def gnUrl = env.getLocalizedUrl();

            if (xpath != null) {
                def image = "<img src=\"${gnUrl}region.getmap.png?mapsrs=$mapproj&amp;width=$width&amp;background=$background&amp;id=metadata:@id$mdId:@xpath$xpath\"\n" +
                        "         style=\"min-width:${width/4}px; min-height:${width/4}px;\" />"

                def inclusion = el.'gex:extentTypeCode'.text() == '0' ? 'exclusive' : 'inclusive';

                def label = f.nodeLabel(el) + " (" + f.translate(inclusion) + ")"
                handlers.fileResult('html/2-level-entry.html', [label: label, childData: image])
            }
        }
    }

    def bboxEl(thumbnail) {
        return { el ->
            if (el.parent().'gex:EX_BoundingPolygon'.text().isEmpty() &&
                    el.parent().parent().'gex:geographicElement'.'gex:EX_BoundingPolygon'.text().isEmpty()) {

                def inclusion = el.'gex:extentTypeCode'.text() == '0' ? 'exclusive' : 'inclusive';

                def label = f.nodeLabel(el) + " (" + f.translate(inclusion) + ")"

                def replacements = bbox(thumbnail, el)
                replacements['label'] = label
                replacements['pdfOutput'] = commonHandlers.func.isPDFOutput()
                replacements['gnUrl'] = env.getLocalizedUrl();

                handlers.fileResult("html/bbox.html", replacements)
            }
        }
    }

    def bbox(thumbnail, el) {
        def mapConfig = env.mapConfiguration
        if (thumbnail) {
            mapConfig.setWidth(mapConfig.thumbnailWidth)
        }

        return [ w: el.'gex:westBoundLongitude'.'gco:Decimal'.text(),
                 e: el.'gex:eastBoundLongitude'.'gco:Decimal'.text(),
                 s: el.'gex:southBoundLatitude'.'gco:Decimal'.text(),
                 n: el.'gex:northBoundLatitude'.'gco:Decimal'.text(),
                 geomproj: "EPSG:4326",
                 minwidth: mapConfig.getWidth() / 4,
                 minheight: mapConfig.getWidth() / 4,
                 mapconfig: mapConfig
        ]
    }
    def rootPackageEl = {
        el ->
            def rootPackage = el.children().findAll { ch -> !this.packageViews.contains(ch.name()) }
            def otherPackage = el.children().findAll { ch -> this.packageViews.contains(ch.name()) }

            def rootPackageData = handlers.processElements(rootPackage, el);
            def otherPackageData = handlers.processElements(otherPackage, el);

            def rootPackageOutput = handlers.fileResult('html/2-level-entry.html',
                    [label: f.nodeLabel(el), childData: rootPackageData, name: rootEl.replace(":", "_")])

            return  rootPackageOutput.toString() + otherPackageData
    }

    def timePeriodPositionEl = { el ->
        def date = el.text()
        def indPosition = el.'@indeterminatePosition'.text()
        handlers.fileResult('html/text-el.html', [
            label: f.nodeLabel(el),
            text: !indPosition.isEmpty() ? f.codelistValueLabel("indeterminatePosition", indPosition) : date
        ])
    }

    // Sextant Specific : Formatters
    def keywordsElSxt = {keywords ->
        def keywordProps = com.google.common.collect.Maps.newHashMap()
        keywords.collectNested {it.'**'.findAll{it.name() == 'mri:keyword'}}.flatten().each { k ->
            def thesaurusName = isofunc.isoText(k.parent().'mri:thesaurusName'.'cit:CI_Citation'.'cit:title')

            if (thesaurusName.isEmpty()) {
                def keywordTypeCode = k.parent().'mri:type'.'mri:MD_KeywordTypeCode'
                if (!keywordTypeCode.isEmpty()) {
                    thesaurusName = f.translate("uncategorizedKeywords")
                }
            }

            if (thesaurusName.isEmpty()) {
                thesaurusName = f.translate("noThesaurusName")
            }
            def keyValue = isofunc.isoText(k);
            if(!keyValue) keyValue = k.'gcx:Anchor'.text()

            def thesaurusIdEl = k.parent().'mri:thesaurusName'.'cit:CI_Citation'.'cit:identifier'.'mcc:MD_Identifier'
            def thesaurusId = isofunc.isoText(thesaurusIdEl.'mcc:code')
            if(!thesaurusId) thesaurusId = thesaurusIdEl.'mcc:code'.'gcx:Anchor'.text()

            if (!keywordProps.get(thesaurusName) && keyValue) {
              keywordProps.put(thesaurusName, [ words: new ArrayList(), thesaurusId: thesaurusId ])
            }

            if(keyValue) keywordProps.get(thesaurusName).get('words').push(keyValue)
        }

        if(keywordProps.size() > 0)
            return handlers.fileResult('html/sxt-keyword.html', [
                    label : f.nodeLabel("mri:descriptiveKeywords", null),
                    keywords: keywordProps])
    }


  def bboxElSxt(thumbnail) {
        return { el ->
            if (el.parent().'gex:EX_BoundingPolygon'.text().isEmpty() &&
                    el.parent().parent().'gex:geographicElement'.'gex:EX_BoundingPolygon'.text().isEmpty()) {
                def replacements = bbox(thumbnail, el)
                replacements['label'] = f.nodeLabel(el)
                replacements['gnUrl'] = env.getLocalizedUrl();
                //replacements['pdfOutput'] = env.formatType == FormatType.pdf

                handlers.fileResult("html/sxt-bbox.html", replacements)
            }
        }
    }

    def dataQualityInfoElSxt =  { el ->
        return handlers.fileResult('html/sxt-statements.html', [
                statements : el
        ])
    }

    def datesElSxt =  { els ->
        def dates = ''
        els.each { el ->
            def date = el.'cit:date'.'gco:Date'.text().isEmpty() ?
                    el.'cit:date'.'gco:DateTime'.text() :
                    el.'cit:date'.'gco:Date'.text()

            if(date) {
                def dateType = f.codelistValueLabel(el.'cit:dateType'.'cit:CI_DateTypeCode')
                dates += '<p>' + date + ' - ' + dateType + '</p>'
            }
        }
        return dates
    }

    def getContactMails = { parent ->
        def mails = []
        parent.'cit:contactInfo'.'cit:CI_Contact'
                .'cit:address'.'cit:CI_Address'.'cit:electronicMailAddress'.each {
            m ->
                def mail = this.isofunc.isoText(m)
                if(mail != "") mails.push(mail)
        }
        return mails
    }

    def addContact = { contacts, contact ->
        // Check that name already in list
        def exist = false
        contacts.each {c ->
            if (c.name == contact.name && c.mail == contact.mail) {
                exist = true
            }
        }
//          TODO: Could be relevant to group contact with same name and
//          different roles or orgs
        if (!exist) {
            if(contact.name != "" || contact.org != '') {
                contacts.push(contact)
            }
        }
    }

    def contactsElSxt =  { responsibilities ->
        def contacts = []
        responsibilities.each { responsibility ->
            responsibility.'cit:party'.each { party ->
                def orgs = party.'cit:CI_Organisation'
                def individuals = party.'cit:CI_Individual'

                orgs.each { org ->
                    def orgIndividual = org.'cit:individual'.'cit:CI_Individual'
                    def orgName = this.isofunc.isoText(org.'cit:name')
                    def orgMails = getContactMails(org)

                    orgIndividual.each { ind ->
                        def name = this.isofunc.isoText(ind.'cit:name')
                        def mails = getContactMails(ind)
                        if(mails.size() == 0) mails = orgMails

                        def contact = [
                            name : name,
                            link: responsibility['@uuid'],
                            emptyName : name == '',
                            mail : mails.join(','),
                            org : orgName
                        ]
                        addContact(contacts, contact)
                    }

                    if(!orgIndividual || orgIndividual == "") {
                        def contact = [
                            name : '',
                            link: responsibility['@uuid'],
                            emptyName : true,
                            mail : orgMails.join(','),
                            org : orgName
                        ]
                        addContact(contacts, contact)
                    }
                }

                individuals.each { ind ->
                    def name = this.isofunc.isoText(ind.'cit:name')
                    def mails = getContactMails(ind)

                    def contact = [
                      name : name,
                      link: responsibility['@uuid'],
                      emptyName : name == '',
                      mail : mails.size() > 0 ? mails.join(',') : ''
                    ]
                    addContact(contacts, contact)
                }
            }
        }
        def replacements = [
                contacts : contacts
        ]

        if(!contacts)  {
            return ""
        }
        else {
            return handlers.fileResult("html/sxt-contacts.html", replacements)
        }
    }

    def constraintsElSxt = { els ->
        def useLimitation
        def accessConstraints
        def useConstraints
        def otherConstraints
        def useLimitationLabel
        def accessConstraintsLabel
        def useConstraintsLabel
        def otherConstraintsLabel

        els.each { el ->
            useLimitationLabel = f.nodeLabel(el."mco:useLimitation")
            accessConstraintsLabel = f.nodeLabel(el."mco:accessConstraints")
            useConstraintsLabel = f.nodeLabel(el."mco:useConstraints")
            otherConstraintsLabel = f.nodeLabel(el."mco:otherConstraints")

            useLimitation = this.isofunc.isoText(el."mco:useLimitation")
            accessConstraints = f.codelistValueLabel(el."mco:accessConstraints"."mco:MD_RestrictionCode")
            useConstraints = f.codelistValueLabel(el."mco:useConstraints"."mco:MD_RestrictionCode")
            otherConstraints = []
            el.collectNested {it.'**'.findAll{it.name() == 'mco:otherConstraints'}}.flatten().each { k ->
                otherConstraints.add(this.isofunc.isoText(k))
            }
        }

        def replacements = [
                accessConstraints: '',
                useLimitation: '',
                useConstraints: '',
                otherConstraints: '',
                useLimitationLabel : useLimitationLabel,
                accessConstraintsLabel : accessConstraintsLabel,
                useConstraintsLabel : useConstraintsLabel,
                otherConstraintsLabel : otherConstraintsLabel
        ]
        def hasContraints = false;

        if (accessConstraints != '') {
            replacements.accessConstraints = accessConstraints
            hasContraints = true;
        }
        if (useLimitation != '') {
            replacements.useLimitation = useLimitation
            hasContraints = true;
        }
        if (useConstraints != '') {
            replacements.useConstraints = useConstraints
            hasContraints = true;
        }
        if (otherConstraints.size() > 0) {
            replacements.otherConstraints = otherConstraints
            hasContraints = true;
        }

        return hasContraints ? handlers.fileResult("html/sxt-constraints.html", replacements) : ''
    }

}
