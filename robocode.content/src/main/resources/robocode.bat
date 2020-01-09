@REM
@REM Copyright (c) 2001-2020 Mathew A. Nelson and Robocode contributors
@REM All rights reserved. This program and the accompanying materials
@REM are made available under the terms of the Eclipse Public License v1.0
@REM which accompanies this distribution, and is available at
@REM https://robocode.sourceforge.io/license/epl-v10.html
@REM

@pushd "%~dp0"
java -Xmx512M -cp libs/robocode.jar -XX:+IgnoreUnrecognizedVMOptions "--add-opens=java.base/sun.net.www.protocol.jar=ALL-UNNAMED" "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED" "--add-opens=java.desktop/javax.swing.text=ALL-UNNAMED" "--add-opens=java.desktop/sun.awt=ALL-UNNAMED" "--add-opens=java.desktop/java.awt=ALL-UNNAMED" "--add-opens=java.base/java.lang=ALL-UNNAMED" "--add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED" "--add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED" robocode.Robocode %*
@popd
