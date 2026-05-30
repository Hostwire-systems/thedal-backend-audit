@echo off
echo Loading environment variables from .env.production...

for /f "usebackq tokens=1,2 delims==" %%a in (".env.production") do (
    if not "%%a"=="" if not "%%a:~0,1%"=="#" (
        set "%%a=%%b"
        echo Set %%a
    )
)

echo Environment variables loaded successfully!
echo You can now run: mvn spring-boot:run

cd thedal-app
mvn spring-boot:run
