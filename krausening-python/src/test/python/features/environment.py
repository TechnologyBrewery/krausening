import os

def after_feature(context, feature):
    if 'properties' == feature.name:
        os.environ['KRAUSENING_BASE'] = None
        os.environ['KRAUSENING_EXTENSION'] = None