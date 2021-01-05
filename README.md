# MainActivity
This activity gets the proper permissions needed.  It also connects to the azure cloud.
# GlobalClass
This class is used to share information across multiple activites.
# DirectoryActivity
This activites creates a WeedDetection directory in the device where all application information will be saved.  This activity allows users to create there own directories to store and manage there files in.
# FilesActivity
The files activity allows users to manage there files, and export them to the cloud.  It also has a button that will bring you to the video activity.
# CameraActivity
The camera activity allows you to change the resolution and FPS of the rgb and depth streams.  This information is saved in the GlobalClass.
# VideoActivity
This activity streams form the intel camera, and allows you to take videos.  All videos will be saved in the current working directory.