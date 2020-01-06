#!/bin/sh
################################################################################
# Copyright (c) 2001-2019 Mathew A. Nelson and Robocode contributors
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# https://robocode.sourceforge.io/license/epl-v10.html
################################################################################

pwd=`pwd`
cd "${0%/*}" || exit
java -Dcom.apple.mrj.application.apple.menu.about.name="RobocodeGL" -Dapple.awt.application.name="RobocodeGL" -Xdock:icon=robocode.icns -Xdock:name=RobocodeGL -Xmx512M -cp libs/robocode.jar -XX:+IgnoreUnrecognizedVMOptions "--add-opens=java.base/sun.net.www.protocol.jar=ALL-UNNAMED" "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED" "--add-opens=java.desktop/javax.swing.text=ALL-UNNAMED" "--add-opens=java.desktop/sun.awt=ALL-UNNAMED" "--add-opens=java.desktop/com.apple.eawt" robocode.Robocode "$*"
cd "${pwd}" || exit
