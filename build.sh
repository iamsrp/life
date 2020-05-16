#!/bin/sh

: ${J3D_HOME:=/usr/share/java}

SOURCES="			\
    Action.java			\
    ActionAttack.java		\
    ActionFeed.java		\
    ActionMate.java		\
    ActionPuff.java		\
    ActionSelfDestruct.java	\
    ActionShout.java		\
    ActionSpawn.java		\
    Body.java			\
    Brain.java			\
    Creature.java		\
    CreatureAttackee.java	\
    CreatureAttacker.java	\
    CreatureDead.java		\
    CreatureFeeder.java		\
    CreatureFlocker.java	\
    CreatureHomer.java		\
    CreatureHunter.java		\
    CreatureKitchenSink.java	\
    CreatureMisc.java		\
    CreatureRandom.java		\
    CreatureSheep.java		\
    CreatureWolf.java		\
    Gene.java			\
    GeneAction.java		\
    GeneAttack.java		\
    GeneAttackChaff.java	\
    GeneAttackResponse.java	\
    GeneChaseAttack.java	\
    GeneFamilyFollow.java	\
    GeneFeed.java		\
    GeneFindMate.java		\
    GeneFlocking.java		\
    GeneHoming.java		\
    GeneMate.java		\
    GeneMinSpeed.java		\
    GeneMutation.java		\
    GeneNotFamilyFollow.java	\
    GeneRandomPuffAction.java	\
    GeneSizeFollow.java		\
    GeneSelfDestruct.java	\
    GeneShout.java		\
    GeneShoutResponse.java	\
    GeneSpawn.java		\
    KDTree.java			\
    Properties.java		\
    Stimulus.java		\
    StimulusAttacked.java	\
    StimulusFamilies.java	\
    StimulusNeighbours.java	\
    StimulusSelf.java		\
    StimulusShouts.java		\
    StimulusTimeMillis.java	\
    World.java"

# Clean first
\rm -f classes/*.class

# Now make
exec javac \
    -Xlint:unchecked -Xlint:deprecation \
    -classpath ${J3D_HOME}/j3dcore.jar:${J3D_HOME}/j3dutils.jar:${J3D_HOME}/vecmath.jar \
    -d classes \
    $SOURCES
