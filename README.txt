////////////////////////////////////////////////////////////////
Baseform EpaNet Java Library 1.0 - 2012-02-22
////////////////////////////////////////////////////////////////

Baseform EpaNet Java Library is part of the Baseform EpaNet tool
<http://www.baseform.org/np4/tools/epanetTool.html>.

Baseform EpaNet Java Library is open-source and licensed under the GNU General Public License, or GPL.
See LICENSE.txt for detailed terms and conditions.

///////
FUNCTIONALITY
///////
The library contains the main port of USEPA <http://www.epa.gov/> EpaNet hydraulic and water quality modeling and
simulation toolkit <http://www.epa.gov/nrmrl/wswrd/dw/epanet.html>.
It also contains a port of the EPANET-MSX (Multi-Species eXtension) which adds complex reaction simulation capabilities
between multiple chemical and biological species in both the bulk flow and at the pipe wall.

This Java EpaNet implementation is feature-complete, representing a full port of the currently available versions of
EpaNet Toolkit (2.00.12 - 20/March/2008) and EpaNet MSX extension (1.1.00 - 08/Feb/2011).

The library adds to the original toolkit(s) by providing a more accessible, cross-platform, object-oriented
implementation; some changes have been made on the integration of hydraulic and quality simulation and on the range of
available input and output file formats, which now include:
 - regular .INP files (epanet and msx)
 - xls/xlsx .INP.XLSX files for direct excel versions of the full network model (easier for edit)
 - xml .INP.XML files for XML markup language versions of the full network model (easier to parse and manipulate
 by computers).

Aside from linking the library against your code, the library includes a command-line access mode via
(org.addition.epanet.EPATool) and a simple LaunchPad application for running Epanet and MSX simulations without having
to write any code (org.addition.epanet.ui.EpanetUI).


///////
USAGE/HOW-TO
///////
You by linking and compiling this source code against your project or by using the more full featured web-enabled
Baseform Epanet Tool as described on http://www.baseform.org/np4/tools/epanetTool.html.



///////
RELEASE HISTORY
///////

2017-11-14
Fixed negative values bug on the visual utility EPATool [thanks petacoder75].

2012-11-29
Added leniency for a few inp sections, loading more files now [thanks for your input & models, Mark Morley].

2012-11-27
Better handling of files modified by Epanet's Toolkit [thanks for your input, Antonio del Olmo García].
Workarounds for Java 7 specifics.

2012-10-16
Bug fix on nextHyd/getStep affecting hydraulic fine results on some scenarios [thanks for finding, José Pedro Matos].

2012-10-02
Bug fix on SimulationRule order parsing [thanks for finding and solving, José Pedro Matos].

2012-07-11
Simulation network performance - hydraulics.

2012-07-06
Simulation network performance boost.

2012-07-06
CV pipes and multiple demands IO bugs fixed

2012-06-25
Eliminated nonsense method "Link.setNURoughness(...)"

2012-03-08
Updated ant build script; included binary builds (jar, exe and dmg)

2012-02-22
Included ant build script

2012-02-15
Official launch