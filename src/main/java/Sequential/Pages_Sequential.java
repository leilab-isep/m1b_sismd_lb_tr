package Sequential;

import java.io.FileInputStream;
import java.util.Iterator;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;

class Pages_Sequential implements Iterable<Page_Sequential> {

  private final int maxPages;
  private final String fileName;

  public Pages_Sequential(int maxPages, String fileName) {
    this.maxPages = maxPages;
    this.fileName = fileName;
  }

  private class PageIterator implements Iterator<Page_Sequential> {

    private XMLEventReader reader;
    private int remainingPages;

    public PageIterator() throws Exception {
      remainingPages = maxPages;
      System.out.println("Max pages: "+maxPages);
      reader = XMLInputFactory.newInstance().createXMLEventReader(new FileInputStream(fileName));
    }

    public boolean hasNext() { 
      return remainingPages > 0; 
    }

    public Page_Sequential next() {
      try {
        XMLEvent event;
        String title = "";
        String text = "";
        while (true) {
          event = reader.nextEvent();
          if (event.isStartElement()) {
            if (event.asStartElement().getName().getLocalPart().equals("page")) {
              while (true) {
                event = reader.nextEvent();
                if (event.isStartElement()) {
                  String name = event.asStartElement().getName().getLocalPart();
                  if (name.equals("title"))
                    title = reader.getElementText();
                  else if (name.equals("text")) 
                    text = reader.getElementText();
                } else if (event.isEndElement()) {
                  if (event.asEndElement().getName().getLocalPart().equals("page")) {
                    --remainingPages;
                    return new Page_Sequential(title, text);
                  }
                }
              }
            }
          }
        }
      } catch (Exception e) {
        //e.printStackTrace(System.out);
        System.out.println("No more pages!");
      }

      remainingPages = 0;
      return null;
    }

    public void remove() { throw new UnsupportedOperationException(); }
  }

  public Iterator<Page_Sequential> iterator() {
    try {
      return new PageIterator();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}