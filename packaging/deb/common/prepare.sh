#!/bin/bash
set -xe
# $1 - source dir root
# $2 - $1/debian - just configurable for reuse
# $3 - service definition file
# $4 - service definition target during install
# $5 - build dependencies file
SOURCE_DIR=$1
VERSION=$(${SOURCE_DIR}/../../rpm/version)
HC2VPP_VERSION=$(${SOURCE_DIR}/../../rpm/hc2vpp_version)
RELEASE=$(${SOURCE_DIR}/../../rpm/release)
BUILD_DIR=${SOURCE_DIR}/honeycomb-${VERSION}
VPP_DEPENDENCIES=$(${SOURCE_DIR}/../../deb/common/vpp_dependencies)
BUILD_DEPENDENCIES=$($5)

# Copy and unpack the archive with vpp-integration distribution
ARCHIVE_DIR=${SOURCE_DIR}/../../../vpp-integration/minimal-distribution/target/
ARCHIVE=${ARCHIVE_DIR}/vpp-integration-distribution-${HC2VPP_VERSION}-hc.tar.gz
cp ${ARCHIVE} ${SOURCE_DIR}
ARCHIVE=${SOURCE_DIR}/vpp-integration-distribution-${HC2VPP_VERSION}-hc.tar.gz
tar -xf ${ARCHIVE} -C ${SOURCE_DIR}/

# Create packaging root
rm -rf ${BUILD_DIR}
mkdir ${BUILD_DIR}

# Copy contents of tar.gz
mv ${SOURCE_DIR}/vpp-integration-distribution-${HC2VPP_VERSION}/ ${BUILD_DIR}/
cp -r $2 ${BUILD_DIR}

# OS service definition
cp ${SOURCE_DIR}/$3 ${BUILD_DIR}

# Changelog file
cat <<EOT >> ${BUILD_DIR}/debian/changelog
honeycomb (${VERSION}-${RELEASE}) unstable; urgency=low

  * 19.01 release

 -- mcmarada <mcmarada@cisco.com>  Thu, 18 Oct 2018 13:42:07 +0200
EOT

# Install instructions
cat <<EOT >> ${BUILD_DIR}/debian/install
vpp-integration-distribution-${HC2VPP_VERSION}/* /opt/honeycomb/
$3 $4
EOT

# Generate control file
cat <<EOT >> ${BUILD_DIR}/debian/control
Source: honeycomb
Section: misc
Priority: optional
Maintainer: fd.io/hc2vpp <hc2vpp@lists.fd.io>
Build-Depends: ${BUILD_DEPENDENCIES}
Standards-Version: 3.9.6
Homepage: https://wiki.fd.io/view/Hc2vpp
Vcs-Browser: https://git.fd.io/cgit/hc2vpp/tree/

Package: honeycomb
Architecture: all
Depends: ${VPP_DEPENDENCIES}, openjdk-8-jre-headless
Suggests: vpp-nsh-plugin
Description: Honeycomb agent for VPP
EOT

echo ${BUILD_DIR}