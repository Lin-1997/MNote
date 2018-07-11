var converter = new showdown.Converter ({
	"omitExtraWLInCodeBlocks": true,
	"noHeaderId": true,
	"literalMidWordUnderscores": true,
	"strikethrough": true,
	"tables": true,
	"tasklists": true,
	"smoothLivePreview": true,
	"simpleLineBreaks": true,
	"requireSpaceBeforeHeadingText": true,
	"openLinksInNewWindow": false,
	"completeHTMLDocument": true
});

function compile (text)
{
	document.getElementById ("MNoteEditor").innerHTML
		= converter.makeHtml (text);
}