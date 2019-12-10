@echo off

REM
REM This file is part of OCaml-Java.
REM Copyright (C) 2007-2014 Xavier Clerc.
REM
REM OCaml-Java is free software; you can redistribute it and/or modify
REM it under the terms of the GNU Lesser General Public License as published by
REM the Free Software Foundation; either version 3 of the License, or
REM (at your option) any later version.
REM
REM OCaml-Java is distributed in the hope that it will be useful,
REM but WITHOUT ANY WARRANTY; without even the implied warranty of
REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
REM GNU Lesser General Public License for more details.
REM
REM You should have received a copy of the GNU Lesser General Public License
REM along with this program.  If not, see <http://www.gnu.org/licenses/>.
REM

if "%JAVA_HOME%"=="" goto empty_java_home
set OCJ_JAVA=%JAVA_HOME%/bin/java.exe
goto run

:empty_java_home
set OCJ_JAVA=java.exe

:run
"%OCJ_JAVA%" -Xss8M -jar %~dp0/../lib/ocamlrun.jar %*
