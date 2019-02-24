package iso19115_3

import org.fao.geonet.api.records.formatters.FormatType
import org.fao.geonet.api.records.formatters.groovy.Environment
import org.fao.geonet.api.records.formatters.groovy.util.*
import org.fao.geonet.domain.ISODate
import java.text.SimpleDateFormat

/**
 * Creates the {@link org.fao.geonet.services.metadata.format.groovy.util.Summary} instance for the iso19139 class.
 *
 * @author Fgravin on 28/03/2015.
 */
class SxtSummaryFactory {
  def isoHandlers;
  org.fao.geonet.api.records.formatters.groovy.Handlers handlers
  org.fao.geonet.api.records.formatters.groovy.Functions f
  Environment env

  def navBarItems

  /*
   * This field can be set by the creator and provided a closure that will be passed the summary object.  The closure can
   * perform customization for its needs.
   */
  Closure<Summary> summaryCustomizer = null

  SxtSummaryFactory(isoHandlers, summaryCustomizer) {
    this.isoHandlers = isoHandlers
    this.handlers = isoHandlers.handlers;
    this.f = isoHandlers.f;
    this.env = isoHandlers.env;
    this.navBarItems = []
    this.summaryCustomizer = summaryCustomizer;
  }
  SxtSummaryFactory(isoHandlers) {
    this(isoHandlers, null)
  }

  static void summaryHandler(select, isoHandler) {
    def factory = new SxtSummaryFactory(isoHandler)
    factory.handlers.add name: "Summary Handler", select: select, {factory.create(it).getResult()}
  }

  SxtSummary create(metadata) {

    SxtSummary summary = new SxtSummary(this.handlers, this.env, this.f)

    summary.title = this.isoHandlers.isofunc.isoText(metadata.'mdb:identificationInfo'.'*'.'mri:citation'.'cit:CI_Citation'.'cit:title')
    summary.abstr = this.isoHandlers.isofunc.isoText(metadata.'mdb:identificationInfo'.'*'.'mri:abstract').replaceAll("\n", "<br>")

    configureKeywords(metadata, summary)
    //configureFormats(metadata, summary)
    configureExtent(metadata, summary)
    configureThumbnails(metadata, summary)
    configureDataQualityInfo(metadata, summary)
    configureDates(metadata, summary)
    configureContacts(metadata, summary)
    configureConstraints(metadata, summary)
    configureDoi(metadata, summary)
    //createCollapsablePanel()

    summary.associated.add(isoHandlers.commonHandlers.loadHierarchyLinkBlocks())
    //summary.associated.add(createDynamicAssociatedHtml(summary))

    def toNavBarItem = {s ->
      def name = f.nodeLabel(s, null)
      def abbrName = f.nodeTranslation(s, null, "abbrLabel")
      new NavBarItem(name, abbrName, '.' + s.replace(':', "_"))
    }

    summary.navBar = this.isoHandlers.packageViews.findAll{navBarItems.contains(it)}.collect (toNavBarItem)
    summary.navBarOverflow = new  ArrayList<String>()
    summary.content = this.isoHandlers.rootPackageEl(metadata)

    if (summaryCustomizer != null) {
      summaryCustomizer(summary);
    }

    return summary
  }

  def configureKeywords(metadata, summary) {
    def keywords = metadata."**".findAll{it.name() == 'mri:descriptiveKeywords'}
    if (!keywords.isEmpty() && keywords.get(0)) {
      def ks = this.isoHandlers.keywordsElSxt(keywords)
      if(ks) summary.keywords = ks.toString()
    }
  }
  def configureFormats(metadata, summary) {
    def formats = metadata."**".findAll this.isoHandlers.matchers.isFormatEl
    if (!formats.isEmpty()) {
      summary.formats = this.isoHandlers.formatEls(formats).toString()
    }
  }
  def configureDates(metadata, summary) {
    def dates = metadata.'mdb:identificationInfo'.'*'.'cit:citation'."**".findAll{it.name() == 'cit:CI_Date'}
    if (!dates.isEmpty()) {
      summary.dates = this.isoHandlers.datesElSxt(dates).toString()
    }
  }

  def configureContacts(metadata, summary) {
    def contacts = metadata.'mdb:identificationInfo'."**".findAll{it.name() == 'cit:CI_Responsibility'}
    if (!contacts.isEmpty()) {
      summary.contacts = this.isoHandlers.contactsElSxt(contacts).toString()
    }
  }

  def configureConstraints(metadata, summary) {
    def constraints = metadata."**".findAll{it.name() == 'mco:MD_LegalConstraints'}
    if (!constraints.isEmpty()) {
      summary.constraints = this.isoHandlers.constraintsElSxt(constraints).toString()
    }
  }

  def configureExtent(metadata, summary) {
    def extents = metadata."**".findAll { this.isoHandlers.matchers.isPolygon(it) || this.isoHandlers.matchers.isBBox(it) }
    def split = extents.split this.isoHandlers.matchers.isPolygon

    def polygons = split[0]
    def bboxes = split[1]

    def extent = ""
    if (!polygons.isEmpty()) {
      extent = this.isoHandlers.polygonEl(true)(polygons[0]).toString()
    } else if (!bboxes.isEmpty()) {
      extent = this.isoHandlers.bboxElSxt(true)(bboxes[0]).toString()
    }
    summary.extent = extent
  }

  def configureDataQualityInfo(metadata, summary) {
    def statementsElts = metadata."**".findAll{it.name() == 'mdq:statement'}
    def statementsString = []
    statementsElts.collectNested {metadata.'**'.findAll{it.name() == 'mdq:statement'}}.flatten().each { k ->
      statementsString.add(isoHandlers.commonHandlers.func.htmlNewLine(this.isoHandlers.isofunc.isoText(k)))
    }

    if (!statementsString.isEmpty() && statementsString.get(0)) {
      def statements = this.isoHandlers.dataQualityInfoElSxt(statementsString).toString();
      summary.formats = isoHandlers.commonHandlers.func.urlToHtml(statements)
    }
  }

  def getContactsByType(metadata, type) {
    return metadata."mdb:identificationInfo"."**".findAll{
      it.name() == 'cit:CI_Responsibility' &&
        (it.'cit:role'.'cit:CI_RoleCode'['@codeListValue'] == type)}
  }

  def containsString(list, value) {
    for(String str: list) {
      if(str.trim().contains(value))
        return true;
    }
    return false;
  }

  def configureDoi(metadata, summary) {
    def doiElts = metadata."**".findAll{
      it.name() == 'cit:CI_OnlineResource' &&
        it.'cit:protocol'.'gco:CharacterString' == 'WWW:LINK-1.0-http--metadata-URL'
    }
    def mainPublicationElts = metadata."**".findAll{
      it.name() == 'cit:CI_OnlineResource' &&
        it.'cit:protocol'.'gco:CharacterString' == 'WWW:LINK-1.0-http--publication-URL'
    }

    if(doiElts.size() < 1 && mainPublicationElts.size() < 1) {
      summary.citation = ''
      return
    }

    def mainPublicationDescription = null
    if (mainPublicationElts.size() >= 1) {
      mainPublicationDescription = []
      mainPublicationElts.forEach{it ->
        def text = it.'cit:description'.'gco:CharacterString'.text()
        mainPublicationDescription.push(
          isoHandlers.commonHandlers.func.urlToHtml(text))
      }
    }

    def el = doiElts[0]

    // contacts: all author, if none all originators
    def contactElts = getContactsByType(metadata, 'author')

    if(contactElts.size() == 0 ) {
      contactElts = getContactsByType(metadata, 'originator')
    }
    def contacts = [];
    contactElts.forEach{it ->
      def name = it.'gmd:individualName'.'gco:CharacterString'.text()
      if(name == null || name == "") {
        name = it.'gmd:organisationName'.'gco:CharacterString'.text()
      }
      if(name != null && !name.equals('')) {
        if(!containsString(contacts, name)) {
          contacts.push(name)
        }
      }
    }

    def publishers = [];
    def publisherElts = getContactsByType(metadata, 'publisher')
    publisherElts.forEach{it ->
      def name = it.'gmd:organisationName'.'gco:CharacterString'.text()
      if(name != null && !name.equals('')) {
        if(!containsString(publishers, name)) {
          publishers.push(name)
        }
      }
    }
    publishers = publishers.join(',')
    if(!publishers.equals('')) {
      publishers += '.'
    }



    def dateElts = metadata.'mdb:identificationInfo'.'*'.'cit:citation'."**".findAll{
                    it.name() == 'cit:CI_Date' &&
                    it.'cit:dateType'.'cit:CI_DateTypeCode'['@codeListValue'] == 'publication'}

    if(dateElts.size() <= 0) {
      dateElts = metadata.'mdb:identificationInfo'.'*'.'cit:citation'."**".findAll{
                    it.name() == 'cit:CI_Date' &&
                    it.'cit:dateType'.'cit:CI_DateTypeCode'['@codeListValue'] == 'creation'}
    }

    def year = '';
    if(dateElts.size() > 0) {
      def sDate = dateElts[0].'cit:date'.'gco:Date'.text();
      if(sDate == null || sDate == "") {
        sDate = dateElts[0].'cit:date'.'gco:DateTime'.text();
      }
      if(sDate != null && sDate != "") {
        def pattern = "yyyy-MM-dd"
        if(sDate.size() == 7) {
          pattern = "yyyy-MM"
        } else if(sDate.size() == 4) {
          pattern = "yyyy"
        }
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        Date date = format.parse(sDate);
        SimpleDateFormat df = new SimpleDateFormat("yyyy");
        year = df.format(date);
      }
    }

    def url = ''
    if(el) {
      url = el.'cit:linkage'.'gco:CharacterString'
    }

    def replacements
    replacements = [
      url: url,
      year: year,
      title: summary.title,
      citationPOTitle: this.f.translate('citationPOTitle'),
      citationPOContent: this.f.translate('citationPOContent'),
      contacts: String.join(', ', contacts),
      publishers: publishers,
      mainPublicationDescription: mainPublicationDescription
    ]

    summary.citation = this.handlers.fileResult("html/sxt-citation.html", replacements)
  }

  def createCollapsablePanel() {

/*
        def js = this.handlers.fileResult("js/utils.js", null)
        def htmlOrXmlEnd = {
            def required = """
            <script type="text/javascript">
            //<![CDATA[
                gnFormatter.formatterOnComplete();

            $js
                //]]></script>
            """
        }
        handlers.end htmlOrXmlEnd
*/
  }

  private static void configureThumbnails(metadata, header) {
    def logos = metadata.'mdb:identificationInfo'.'*'.'mri:graphicOverview'.'mcc:MD_BrowseGraphic'.'mcc:fileName'.'gco:CharacterString'

    logos.each { logo ->
      header.addThumbnail(logo.text())
    }
  }

}
