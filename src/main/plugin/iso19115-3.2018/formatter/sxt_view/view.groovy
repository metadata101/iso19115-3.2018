import iso19115_3.SxtSummaryFactory

def isoHandlers = new iso19115_3.Handlers(handlers, f, env)

SxtSummaryFactory.summaryHandler({it.parent() is it.parent()}, isoHandlers)

isoHandlers.addDefaultHandlers()
