import os
from javaproperties import Properties as JavaProperties
from krausening.logging import LogManager
from krausening.properties import PropertyEncryptor
from typing import Optional, TypeVar
from platform import uname
from watchdog.events import FileSystemEventHandler
from watchdog.observers import Observer


def in_wsl() -> bool:
    return "microsoft-standard" in uname().release


# Use polling observer if in WSL due to watchdog observer currently not correctly detecting changes
if in_wsl():
    from watchdog.observers.polling import PollingObserver as Observer

T = TypeVar("T")


class FileUpdateEventHandler(FileSystemEventHandler):
    def __init__(self, base_path):
        self._logger = LogManager.get_instance().get_logger("FileWatcher")
        self._base_path = os.path.abspath(base_path)
        self._logger.info(f"File Watcher started on {self._base_path}")

    def on_modified(self, event):
        file_path = event.src_path.replace(self._base_path, "")

        ## testing for WSL since watchdog acts differently with WSL else continue as normal
        ## if in WSL we need only the file name and not the whole path
        if in_wsl():
            last_index = file_path.rfind("/")
            if last_index != -1:
                self._logger.info(
                    f"Updating file_path to: {file_path[last_index + 1 :]}"
                )
                file_path = file_path[last_index + 1 :]

        else:
            if file_path.startswith("/"):
                self._logger.info(f"Updating file_path to: {file_path[1:]}")
                file_path = file_path[1:]

        if PropertyManager.get_instance().is_loaded(file_path):
            self._logger.warn(
                f"Detected a file update in {event.src_path}!  Triggering update..."
            )
            PropertyManager.get_instance().get_properties(file_path, force_reload=True)


class PropertyManager:
    """
    Class for handling External Property Configurations.
    """

    __instance = None

    @staticmethod
    def get_instance():
        if PropertyManager.__instance is None:
            PropertyManager()
        return PropertyManager.__instance

    def __init__(self):
        if PropertyManager.__instance is not None:
            raise Exception("Class is a singleton")
        else:
            PropertyManager.__instance = self
        self._logger = LogManager.get_instance().get_logger("PropertyManager")
        self._property_cache = {}

        if os.environ.get("KRAUSENING_BASE", None) is not None:
            self._base_observer = Observer()
            self._base_observer.schedule(
                FileUpdateEventHandler(os.environ.get("KRAUSENING_BASE")),
                os.environ.get("KRAUSENING_BASE"),
                recursive=True,
            )
            self._base_observer.start()

        if (
            os.environ.get("KRAUSENING_EXTENSIONS", None) is not None
            and os.environ.get("KRAUSENING_EXTENSIONS") != ""
        ):
            self._extension_observer = Observer()
            self._extension_observer.schedule(
                FileUpdateEventHandler(os.environ.get("KRAUSENING_EXTENSIONS")),
                os.environ.get("KRAUSENING_EXTENSIONS"),
                recursive=True,
            )
            self._extension_observer.start()

        if (
            os.environ.get("KRAUSENING_OVERRIDE_EXTENSIONS", None) is not None
            and os.environ.get("KRAUSENING_OVERRIDE_EXTENSIONS") != ""
        ):
            self._extension_observer = Observer()
            self._extension_observer.schedule(
                FileUpdateEventHandler(
                    os.environ.get("KRAUSENING_OVERRIDE_EXTENSIONS")
                ),
                os.environ.get("KRAUSENING_OVERRIDE_EXTENSIONS"),
                recursive=True,
            )
            self._extension_observer.start()

    def get_properties(self, file_name: str, force_reload=False):
        if file_name in self._property_cache and not force_reload:
            return self._property_cache[file_name]

        base = os.environ.get("KRAUSENING_BASE", None)
        extension = os.environ.get("KRAUSENING_EXTENSIONS", None)
        override = os.environ.get("KRAUSENING_OVERRIDE_EXTENSIONS", None)
        password = os.environ.get("KRAUSENING_PASSWORD", None)
        if password is not None:
            properties = EncryptableProperties(password)
        else:
            properties = Properties()

        if file_name not in self._property_cache:
            self._property_cache[file_name] = properties

        if base is not None:
            try:
                if not base.endswith("/"):
                    base = base + "/"
                properties.load(open("{0}{1}".format(base, file_name)))
            except FileNotFoundError:
                self._logger.warn(
                    "No base file found for {0}{1}".format(base, file_name)
                )

        if extension is not None:
            try:
                if not extension.endswith("/"):
                    extension = extension + "/"
                properties.load(open("{0}{1}".format(extension, file_name)))
            except FileNotFoundError:
                self._logger.warn(
                    "No extension file found for {0}{1}".format(base, file_name)
                )

        if override is not None:
            try:
                if not override.endswith("/"):
                    override = override + "/"
                properties.load(open("{0}{1}".format(override, file_name)))
            except FileNotFoundError:
                self._logger.warn(
                    "No extension file found for {0}{1}".format(base, file_name)
                )

        self._property_cache[file_name].update(properties)

        return self._property_cache[file_name]

    def is_loaded(self, file_name):
        return file_name in self._property_cache


class Properties(JavaProperties):
    """
    This class represents a properties file without encryption
    """

    def __init__(self) -> None:
        super().__init__()

    def __getitem__(self, key: str) -> str:
        return self.getProperty(key)

    def getProperty(self, key: str, defaultValue: Optional[T] = None):
        try:
            return os.path.expandvars(self.data[key])
        except KeyError:
            if self.defaults is not None:
                return os.path.expandvars(self.defaults.getProperty(key, defaultValue))
            else:
                return os.path.expandvars(defaultValue)


class EncryptableProperties(Properties):
    """
    Provides property value encryption/decryption support via PBEWITHHMACSHA512ANDAES_256. This aligns with
    the same approach used for property encryption within the Krausening Java package.

    Reference: https://resultfor.dev/359470-implement-pbewithhmacsha512andaes-256-of-java-jasypt-in-python.

    See https:https://github.com/TechnologyBrewery/krausening/tree/dev/krausening for details on encrypting values with Jasypt.
    """

    def __init__(self, password: str) -> None:
        super().__init__()
        self.__propertyPrefix = "ENC("
        self.__propertySuffix = ")"
        self.__encryptor = PropertyEncryptor()
        self.__password = password

    def getProperty(self, key: str, defaultValue: Optional[T] = None):
        value = super().getProperty(key, defaultValue)
        if value is not None:
            if value.startswith(self.__propertyPrefix) and value.endswith(
                self.__propertySuffix
            ):
                encrypted_value = value
                # remove prefix from value
                encrypted_value = encrypted_value[len(self.__propertyPrefix) :]
                # remove suffix from value
                encrypted_value = encrypted_value[: -len(self.__propertySuffix)]

                # decrypt the value
                password_bytes = self.__password.encode()
                value = self.__encryptor.decrypt(encrypted_value, password_bytes)

        return os.path.expandvars(value)

    def _encrypt(self, key: str) -> bytes:
        value = self.get(key)
        password_bytes = self.__password.encode()
        encrypted_value = self.__encryptor.encrypt(value, password_bytes)
        return encrypted_value

    def _decrypt(self, value: str):
        password_bytes = self.__password.encode()
        decrypted_value = self.__encryptor.decrypt(value, password_bytes)
        return decrypted_value
