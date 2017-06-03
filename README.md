# life

Background
==========

This is just something that I find myself playing about with from time to
time. This implementation is in Java but previous incarnations have mostly
been C++; however this is probably the cleanest one.

The use of the 3D interface is a little rough and ready but it seems to work
well enough.

You'll need Java3d to build it. It should work on both Linux and Windows.

Finally, this code is over 10 years old at the time of its "initial"
commit. Read into that what you will...

What's happening
================

The basic idea of Life is that we have a physical world with various
properties and laws of motion (friction, gravity, etc.) and creatures bimbling
about in that world trying to survive. The creatures have the abilty to:
  o Move about (by "puff"ing in the other direction)
  o Attack each other
  o Feed (off dead creatures)
  o Mate (sexually or asexually)
  o Attack each other
  o Shout/hear things
  o Grow old and die
  o ...

These actions and responses etc. are controlled by genes which each creature
has. You can delve into the code if you care about the details of this; see
the Gene and Stimulus family of classes.

The bigger a creature is, the slower its metabolism runs. This means it will
reproduce more slowly but also age more slowly etc. Also the bigger they are,
the harder they hit; but things like moving about and reproducing require more
energy too. As such there are advantages to being both big and small. (Like
most things, size is controlled by a gene.)

Each creature also has an associated family which it can recognise, these
families are colourised in the display so that the user can see what's going
on. The creatures also have a little "tail" sticking out the back. This has a
green and a red bit. The green bit represents the current health of the
creature, when this falls to zero then the creature is dead. The tail also
helps the user see roughly the direction in which the creature is pointing.

In the world, every now an again, two things happen:
  1. We throw food into the world
  2. We add new creatures to the world (if the population is too diminished)

Also, if one family completely dominates at any time then we split it into
(right now) 4 new families to force evolution along its path.


Comments about the code
=======================

The code was vaguely written with competition between authors in mind (i.e.
people could go off and write their own creatures and genes and then pit them
against one another). As such pains are taking in the code so ensure that you
can't do anything "underhand" like taking another creature instance and
messing about with it's health or whatever. As such there are constructs in
the code (like VisibleCreature) which exist to prevent this and might
otherwise appear a little pointless.

Also, the code is by no means perfect. This really is something I have been
messing about with no real end in mind (I dunno if I'll even feel the urge to
"officially" release it one day) so don't expect much. However, I tried to
keep it pretty clean and comment it well enough so that other people can see
what's going on in there and add to it etc.


Running
=======

You'll need the Java3d environment (http://java.sun.com/products/java-media/3D/)
installed in order for this to run (and Java, of course) but that's about it.
Build with build.sh and run with run.sh; though you might want to edit the max
heap size line if you don't have stacks of memory like wot I does. (I'm
assuming you're on a unix-like box, but there's also run.bat too).

  ./build.sh && ./run.sh

You can also define certain properties on the command line:

  ./run.sh -Dgravity=0.1 -DairFrictionCoeff=150 -DmaxPopulation=1000

See the Properties.java file for the full set. The above makes things a lot
more like an underwater scene (high air friction and low gravity) with lots of
things to play about therein. You'll probably also find that, the more food
you throw into the system, the more passive the creatures are (since they need
to compete less); this can be altered by setting the feed interval property.
In fact, anything which forces competition tends to make things more
aggressive (e.g. high gravity, meaning there is less "space" for things to
live in) and vice versa.


ToDo
====

Have other windows open which give stats on the current families and so on.

Have the ability to restore a world from a previously persisted state.

Turn this into a screen saver (and kill the planet by making everyone's CPUs
grind away to their maximum capacity; I'll probably go to hell as a result).
