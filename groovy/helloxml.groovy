#!/bin/env groovy

def REQUEST = "pCode=2RH31S101XG01&pStart=200910061523&pStop=201103281305"
if (this.args.size()==1) if (this.args[0]!='') REQUEST =  this.args[0]
println """\
<ROWSET>
<!-- this.args:
${this.args}

REQUEST:
$REQUEST

-->
</ROWSET>
"""