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

#MAKE_SILENT=
MAKE_SILENT=-s

KDX_LIBS=kdxos_clientlib kdxos_common kdxos_stats kdxos_ttools kdxos_curcommon

KDXAPPS=kdxapp_welcome kdxapp_trialmgr kdxos_curation kdxos_vistools kdxapp_trialdesign

# kdxos_main is last
KDX_DIRS=$(KDX_LIBS) kdxos_main

KDX_MAKEFILES=$(patsubst %,../%/Makefile,$(KDX_JARS))

ifeq ($(JAVAC_DEBUG_FLAG),)
	JAVAC_DEBUG_FLAG=-g:lines
endif

.PHONY: all
all: kdx_jars kdxapps

.PHONY: kdx_jars
kdx_jars:
	@for d in $(KDX_LIBS); do rm -f kdxplore_os/lib/$$d.jar; done
	@for d in $(KDX_DIRS); do \
	    echo "[Building $$d]" >&2; \
	    if ! make $(MAKE_SILENT) -C $$d; then exit 1; fi; \
	done

.PHONY: kdxapps
kdxapps:
	@for d in $(KDXAPPS); do rm -f kdxplore_os/plugins/$$d.jar; done
	@for d in $(KDXAPPS); do \
	    echo "[Building $$d]" >&2; \
	    if ! make $(MAKE_SILENT) -C $$d; then exit 1; fi; \
	done
