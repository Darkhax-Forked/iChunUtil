Notice: This repo is for PigUtils. PigUtils is a fork of the iChunUtils mod made by iChun. Permission for this fork has been granted under the [LGPL 3.0 license](https://github.com/iChun/iChunUtil/blob/62d4b28809dc5b79940c01b963552fc960272df8/COPYING). You can find the original mod by iChun [here](https://minecraft.curseforge.com/projects/ichunutil). The sources for the original mod by iChun can be found [here](https://github.com/iChun/iChunUtil). The sources for the PigUtils fork can be found [here](https://github.com/Darkhax-Forked/iChunUtil). All of the changes I have made to his project can be seen in the GitHub commit history.

**If you run into any issues while using this fork, you should report them to this project instead of iChun's iChunUtils pages. iChun is not responsible for issues caused by changes I have made.**

## Original README

If you have an error with protected/private access etc, you need to put /src/main/resources/META-INF/iChunUtil_at.cfg into Forge's src folder (same folder structure) and rerun setupDecompWorkspace. Make sure the Forge setup finds the AT config or you're doing it wrong.

How to set up this and another mod of mine to be compiled:
1. Clone this repository.
2. Due to morph's API requirement, get Morph's API and put it in src/api/java/. Maintain the folder structure
3. Build iChunUtil (use build.bat if you're on Windows for easy double clicky goodness)
4. Go to build/libs/ and copy iChunUtil-<version>-deobf.jar.
5. Clone/set up a gradle project (in a different folder).
6. Put the jar you copied into the libs/ folder.
7. If the module needs any other libraries/APIs, go get them and put them in libs/ or src/api/(java or resources).
8. Build the project.
9. If project fails because protected/private access, copy the META-INF/ folder from iChunUtil and put it in your project's src/api/resources/ folder. Build the project again.
10. Success, hopefully.
