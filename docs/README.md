# 19-kislev-archive #

Archive documents of the arrest of the Alter Rebbe and chassidim of
 Vilno in 1798 and the second arrest of 1800.

Transcripts of the documents, identification of documents,
 suggestions etc. are welcome.

Published using GitHub Pages at <http://www.alter-rebbe.org>


## History ##

Documents were discovered in Central State Archive of the October Revolution
(now - State Archive of the Russian Federation) in 1991 and
photographed by F. Z. Shvaiger on behalf of the Shamir Society.

Rabbi Michael Koretz turned the raw photographs of the documents to me for publication
during a visit to Israel in 2006.

In 2007, the documents were published as a web-site with a content, description and a
few transcripts. The site was backed by a web application written (probably) in Java/XQuery.

In 2008-2009 the site was reworked: it became static, generated from a monolithic metadata
XML document by XSLT code.  

Since original publication, the documents were served from my home server. In 2014, after a
hardware failure took my home server - and the archive - offline, I decided to move the
documents to GitHub Pages.

During the move, the site was reworked again: first, the monolithic metadata document was split,
and finally, removed entirely, with information moved into separate TEI files that now contain
metadata and transcripts of the facsimiles. Those files are primary: index is generated from them
- as it should have been from the beginning :)

Code used for the move and site generation is in Scala. 

To display TEI documents in the browser, together with the original facsimiles,
I use [TEI Boilerplate](http://dcl.ils.indiana.edu/teibp/). Its help was invaluable.

My site is rooted in the `docs` directory; TEI Boilerplate
[repository](https://github.com/GrantLS/TEI-Boilerplate) has a `dist` directory.
This is how I update my setup when new version of TEI Boilerplate gets released:
- copy `teibp.xsl` and `xml-to-string.xsl` from their `content` directory to my `xslt` directory;
- in the `xml-stylesheet` processing instruction, I associate my TEI documents with `teibp-custom.xsl`
  and use that for XSLT customization, so their `custom.xsl` is not needed;
- copy `teibp.css`, `sleepy.css` and `terminal.css` from their `css` directory to mine;
- I set `customCSS` XSL parameter in my `teibp-custom.xsl` to `teibp-custom.css`
  and use that for CSS customization, so their `custom.css` is not needed;
- copy `teibp.js` from their `js` directory to mine;
- in my layout, TEI documents are in the `documents` directory of a collection,
  so the path from a document to `teibp-custom.xsl` is `"../../xslt/teibp-custom.xsl"`,
  and I set `filePrefix` parameter in `teibp-custom.xsl` to `'../..'`.
 