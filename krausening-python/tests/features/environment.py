import os

from krausening.properties import PropertyManager


def before_scenario(context, scenario):
    """
    Clear all Krausening environment variables prior to each scenario.
    """
    os.environ["KRAUSENING_BASE"] = ""
    os.environ["KRAUSENING_EXTENSIONS"] = ""
    os.environ["KRAUSENING_OVERRIDE_EXTENSIONS"] = ""
    os.environ["KRAUSENING_PASSWORD"] = ""
