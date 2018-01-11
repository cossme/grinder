export ANT_HOME=/opt/ant/apache-ant-1.7.1
export JAVA_HOME=/opt/mw-1032/jdk160_14_R27.6.5-32
export PATH=${ANT_HOME}/bin:${JAVA_HOME}/bin:$PATH

# Clover home also needs to be set in localpaths.properties.
CLOVER=/opt/clover/clover-ant-2.0.3/lib/clover.jar
#XALAN=/opt/xalan/xalan-j_2_3_1/bin/xalan.jar:/opt/xalan/xalan-j_2_3_1/bin/xml-apis.jar

export CLASSPATH="${CLOVER}"
