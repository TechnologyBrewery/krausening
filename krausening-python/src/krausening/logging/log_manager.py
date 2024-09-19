import logging


class LogManager:
    """
    Class for handling logging.
    """

    DEFAULT_LOG_LEVEL = "INFO"

    LOG_LEVELS = {
        "CRITICAL": logging.CRITICAL,
        "ERROR": logging.ERROR,
        "WARNING": logging.WARNING,
        "INFO": logging.INFO,
        "DEBUG": logging.DEBUG,
        "NOTSET": logging.NOTSET,
    }

    __instance = None

    __log_format = "%(asctime)s %(levelname)s %(name)s: %(message)s"
    __date_format = "%Y/%m/%d %H:%M:%S"
    __formatter = logging.Formatter(fmt=__log_format, datefmt=__date_format)
    __handler = logging.StreamHandler()
    __handler.setFormatter(__formatter)

    @staticmethod
    def get_instance():
        if LogManager.__instance is None:
            LogManager()
        return LogManager.__instance

    def __init__(self):
        if LogManager.__instance is not None:
            raise Exception("Class is a singleton")
        else:
            LogManager.__instance = self

    def _get_log_level(self, log_level: str) -> int:
        return self.LOG_LEVELS.get(log_level, self.LOG_LEVELS[self.DEFAULT_LOG_LEVEL])

    def get_logger(
        self, name: str, log_level: str = DEFAULT_LOG_LEVEL
    ) -> logging.Logger:
        logger = logging.getLogger(name)
        logger.addHandler(LogManager.__handler)

        log_level = self._get_log_level(log_level)
        logger.setLevel(log_level)

        return logger
