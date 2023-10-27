# SplunkJmeterPlugin

Overview
------------

Simple JMeter plugin that will push live results to a Splunk HEC endpoint.  
You can then view real time results on splunk. 

Installation
------------

Installation is fairly straightforward, and only involves adding the plugin right directory:

1. Build with maven
2. Place the jar file into JMeter's lib/ext directory
4. Run JMeter

Usage
------------

Using the plugin is simple


1. Create a new Test Plan
2. Add a Thread Group
3. Add a backend Listener
4. Specify the host to connect to, port, splunk token <br />
runId	${__UUID()} <br />
dateTimeAppendFormat	-yyyy-MM-DD <br />
splunkHost	 <br />
splunkProxy	 <br />
splunkToken	 <br />
splunkIndex	 <br />
splunkSourceType	_json <br />
retentionPolicy	autogen <br />
5. Run the test


Contributing
------------

1. Fork it.
2. Create a branch (`git checkout -b my_plugin`)
3. Commit your changes (`git commit -am "Added feature"`)
4. Push to the branch (`git push origin my_plugin`)
5. Create an [Issue] with a link to your branch
