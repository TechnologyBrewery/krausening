import logging


class LogManager:
    """
    Class for handling logging.
    """

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

    def get_logger(self, name: str) -> logging.Logger:
        logger = logging.getLogger(name)
        logger.addHandler(LogManager.__handler)
        logger.setLevel(logging.INFO)

        return logger
