#!/usr/bin/python

# Inspiration drawn from o2x (http://www.sabren.com/code/python/)

from changes2xml import XMLOutput, quote

def outline2xml(file):
    output = XMLOutput("todo")

    for line in file.readlines():
        line = quote(line)

        if line[:3] == "-*-": continue        

        if line and line[0] == "*":
            depth = 0
            while line[depth] == "*": depth = depth + 1

            output.closeToDepth(depth)
            
#            for difference in range(output._stack.depth(), depth):
#                output.openSection("", depth=difference)
				
            title = line[depth:].strip()
            output.openSection(title, title, depth = depth)
        else:
            output.addLine(line)
			
    return output.result()
    

if __name__ == "__main__":
    import sys
    for f in sys.argv[1:]:
        print outline2xml(open(f, "r"))
