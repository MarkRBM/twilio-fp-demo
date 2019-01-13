package interfaces

trait ErrorHandler {
  def recovering: PartialFunction[Throwable, Unit]
}
