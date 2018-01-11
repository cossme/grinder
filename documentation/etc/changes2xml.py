#!/usr/bin/python

# Inspiration drawn from o2x (http://www.sabren.com/code/python/)

import re
import subprocess
from subprocess import PIPE

class ElementStack:
    _data = []

    def open(self, tag, **attributes):
        self._data.append(tag)
        result = "<%s" % tag
        for key,value in attributes.items():
            if value:
                result += " %s='%s'" % (key.replace("_", ":"), value)
        return result + ">\n"

    def ensureOpen(self, tag):
        if self._data[-1] != tag:
            return self.open(tag)
        else:
            return ""

    def perhapsClose(self, tagList):
        result = ""
        for t in tagList:
            if self._data[-1] == t:
                result += self.close()

        return result

    def close(self):
        tag  = self._data.pop()
        return "</%s>\n" % tag

    def depth(self):
        return len(self._data)


class XMLOutput:
    _stack = ElementStack()
    _forceParagraph = 1
    _result = '<?xml version="1.0"  encoding="iso-8859-1"?>\n\n'
    _ids = {}

    def __init__(self, topLevelTag):
        self._result += self._stack.open(topLevelTag)

    def addLine(self, line):
        line = line.strip()

        if not line:
            self._forceParagraph = 1
        else:
            if self._forceParagraph:
                self._forceParagraph = 0
                self._result += self._stack.perhapsClose(("li", "ul", "p"))
                self._result += self._stack.ensureOpen("p")

            if line[:2] == "- ":
                self._result += self._stack.perhapsClose(("li", "p"))
                self._result += self._stack.ensureOpen("ul")
                self._result += self._stack.ensureOpen("li")
                self._result += line[1:]
            elif line[:4] == "&gt;":
                self._result += self._stack.perhapsClose(("li"))
                self._result += "<br/>"
                self._result += line
            else:
                self._result += line

            self._result += " "

    def addLiteral(self, element, text, **attributes):
        self._result += self._stack.open(element, **attributes)
        self._result += text # Probably needs quoting
        self._result += self._stack.close()

    def closeToDepth(self, depth):
        for difference in range(depth, self._stack.depth()):
            self._result += self._stack.close()

    def openSection(self, name, identity, **attributes):
        self._result += self._stack.perhapsClose(("li", "ul", "p"))

        self._result += self._stack.open("section",
                                         name=name,
                                         id = self.uniqueID(identity),
                                         **attributes)
        self._forceParagraph = 1

    def result(self):
        self.closeToDepth(0)
        return self._result

    def uniqueID(self, x):
        i = 1
        id = x
        while self._ids.has_key(id):
            id = "%s-%d" % (x, i)
            i += 1
        self._ids[id] = 1

        return id


def quote(line, replaceNewLines=1, hyperlinks=1):
    result = line
    if replaceNewLines:
        result = result.replace(chr(0x0a), "")
    result = result.replace("&", "&amp;")
    result = result.replace("<", "&lt;")
    result = result.replace(">", "&gt;")
    if hyperlinks:
        result = re.sub(r"(http://[^\s]*)", "<a href=\"\\1\">\\1</a>", result)
    result = re.sub(r"([Bb]ug)\s+(\d{6,})", '<a href="http://sourceforge.net/tracker/index.php?func=detail&amp;aid=\\2&amp;group_id=18598&amp;atid=118598">\\1 \\2</a>', result)
    result = re.sub(r"([Rr]equest)\s+(\d{6,})", '<a href="http://sourceforge.net/tracker/index.php?func=detail&amp;aid=\\2&amp;group_id=18598&amp;atid=368598">\\1 \\2</a>', result)
    result = re.sub(r"([Bb]ug)\s+(#?\d{1,5})(?!\d)", '<a href="http://sourceforge.net/p/grinder/bugs/\\2">\\1 \\2</a>', result)
    result = re.sub(r"([Rr]equest)\s+(#?\d{1,5})(?!\d)", '<a href="http://sourceforge.net/p/grinder/feature-requests/\\2">\\1 \\2</a>', result)

    return result


def findRelease(line):
    version = re.search(".*Grinder (.*)", line)

    if not version:
        return None, None

    version = version.group(1)


    def findReleaseTag(version):
        return subprocess.Popen(["git", "tag", "-l", "release_%s" % version],
                                stdout=PIPE).stdout.read().strip()

    tag = findReleaseTag(version)

    if not tag:
        tag = findReleaseTag(version.replace(".", "_").replace("-", "_"))

    date = None
    if tag:
        p2 = subprocess.Popen(["git", "log", "-1", "--pretty=format:%ad",
                               "--date=short", tag],
                              stdout=PIPE)
        date = p2.stdout.read().strip()
    return tag, date


def changes2xml(file):
    output = XMLOutput("changes")

    for line in file.readlines():
        line = quote(line)

        if line[:1] == "-": continue

        if line and line[0] != " " and line[0] != "\t":
            output.closeToDepth(1)
            line = line.strip()
            identity, date = findRelease(line)

            output.openSection(line, identity or "id", date=date)
        else:
            output.addLine(line)
            pass

    return output.result()


if __name__ == "__main__":
    import sys
    for f in sys.argv[1:]:
        print changes2xml(open(f, "r"))
