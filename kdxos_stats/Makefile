# KDXplore provides KDDart Data Exploration and Management
# Copyright (C) 2015,2016  Diversity Arrays Technology, Pty Ltd.
#
# KDXplore may be redistributed and may be modified under the terms
# of the GNU General Public License as published by the Free Software
# Foundation, either version 3 of the License, or (at your option)
# any later version.
#
# KDXplore is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with KDXplore.  If not, see <http://www.gnu.org/licenses/>.

SPACE :=
SPACE += 

LIBS_DIR=../kdxos_libs/libs
KDXPLORE_OS=../kdxplore_os

JAR_DESTDIR=$(KDXPLORE_OS)/lib

LIB_JARS=collections-generic-4.01.jar \
	pearcan-core.jar \
	ssupport.jar \
	jfreechart-1.0.19.jar \
	ormlite-core-4.48.jar \
        kdx_database.jar

KDX_JARS=kdxos_common.jar
KDX_JARPATH=$(subst $(SPACE),:,$(patsubst %,$(JAR_DESTDIR)/%,$(KDX_JARS)))

CLASSPATH=$(subst $(SPACE),:,$(patsubst %,$(LIBS_DIR)/%,$(LIB_JARS))):$(KDX_JARPATH)

CLASSES=./build/classes

JAVA_SRC=src/main/java
RESOURCES=src/main/resources

SRCDIRS=$(JAVA_SRC) $(RESOURCES)

JAVAC_ARGS=$(JAVAC_DEBUG_FLAG) -d $(CLASSES) \
		-classpath $(CLASSPATH) \
		-sourcepath $(subst $(SPACE),:,$(SRCDIRS))

.PHONY: kdxos_stats.jar
kdxos_stats.jar:
	rm -rf $(CLASSES)
	mkdir -p $(CLASSES)
	if javac $(JAVAC_ARGS) `find $(JAVA_SRC) -name '*.java'`; \
	then \
		if test -d $(RESOURCES); then cp -r $(RESOURCES)/* $(CLASSES); else echo >&2 "?Missing $(RESOURCES)"; exit 1 ; fi; \
		jar cf $(JAR_DESTDIR)/$@ -C $(CLASSES) . && echo "Finished $@"; \
	else echo javac $(JAVAC_ARGS); fi;
